package com.example.demo.websocket;

import com.example.demo.backpressure.BackpressureSamplingService;
import com.example.demo.client.OllamaClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class BroadcastService {
    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);
    @Autowired
    ObjectMapper mapper;
    @Autowired
    OllamaClient client;
//    private final ExecutorService executorService = ForkJoinPool.commonPool();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
//    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
//    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private final List<WebSocketSession> allSessions = new CopyOnWriteArrayList<>();
    private final BackpressureSamplingService samplingService;
    private final String prompt;
    private final boolean ollamaEnabled;

    public BroadcastService(BackpressureSamplingService samplingService,
                            @Value("${ollama.prompt.file}") String promptPath,
                            @Value("${ollama.enabled}") boolean ollamaEnabled
                            ) throws IOException {
        this.samplingService = samplingService;
        this.ollamaEnabled = ollamaEnabled;
        this.prompt = Files.readString(ResourceUtils.getFile(promptPath).toPath());
        log.info("using prompt path: {}, content: {}", promptPath, prompt);
    }

    @Timed
    public List<Messages.OnlineUser> registerSession(WebSocketSession session) {
        allSessions.add(session);
        byte[] payload = toStringValue(new Messages.OnlineStatusChange((String) session.getAttributes().get("username"), true, allSessions.size()));
        TextMessage message = new TextMessage(payload);
        sendAll(message, session);
        return getActiveUsers();
    }

    private List<Messages.OnlineUser> getActiveUsers() {
        return allSessions.stream()
                .map(s -> new Messages.OnlineUser((String) s.getAttributes().get("username")))
                .toList();
    }

    @Timed
    public void unregisterSession(WebSocketSession session) {
        allSessions.remove(session);
        byte[] payload = toStringValue(new Messages.OnlineStatusChange((String) session.getAttributes().get("username"), false, allSessions.size()));
        TextMessage message = new TextMessage(payload);
        sendAll(message, session);
    }

    @Timed
    public CompletableFuture send(WebSocketSession senderSession, Messages.ContributionMessage contribution) {
        if (contribution.type() == Messages.MessageType.VIDEO_STOPPED) {

            Messages.VideoStoppedMessage update = new Messages.VideoStoppedMessage(
                    (String) senderSession.getAttributes().get("username")
            );
            byte[] payload = toStringValue(update);
            TextMessage message = new TextMessage(payload);
            return sendAll(message, senderSession);
        }

        var id = (UUID) senderSession.getAttributes().get("id");
        if (ollamaEnabled) {
            samplingService.sample(id, () -> {
                String base64 = contribution.videoStream().substring(contribution.videoStream().lastIndexOf(",") + 1);
                String answer = client.ask(prompt,
                        base64);

                log.info("Got answer: {}", answer);
                Messages.VideoFeedbackMessage update = new Messages.VideoFeedbackMessage(
                        (String) senderSession.getAttributes().get("username"),
                        answer
                );
                byte[] payload = toStringValue(update);
                TextMessage message = new TextMessage(payload);
                sendAll(message)
                        .join();
            });
        }

        Messages.VideoMessage update = new Messages.VideoMessage(
                (String) senderSession.getAttributes().get("username"),
                contribution.videoStream()
        );
        byte[] payload = toStringValue(update);
        TextMessage message = new TextMessage(payload);
        return sendAll(message, senderSession);
    }

    private CompletableFuture sendAll(TextMessage message) {
        return sendAll(message, null);
    }
    private CompletableFuture sendAll(TextMessage message, WebSocketSession senderSessionToSkip) {
//        TODO send sequentially
        allSessions.forEach(session -> {
                    try {
                        if ( senderSessionToSkip == null || !senderSessionToSkip.equals(session)) {
                            if (session.isOpen())
                                session.sendMessage(message);
                            else
                                unregisterSession(session);
                        }
                    } catch (IOException e) {
                        log.warn("send - error", e);
                        // TODO remove session from list
                    }
                }
        );
        return CompletableFuture.allOf();
//        TODO try in parallel OOM killed after 10 rate with 10 instances
//        allSessions.forEach(session -> {
//                    if (senderSessionToSkip == null || !senderSessionToSkip.equals(session) ) {
//                        executorService.submit(() -> {
//                            try {
//                                session.sendMessage(message);
//                                return session;
//                            } catch (IOException e) {
//                                log.warn("send - error", e);
//                                // TODO remove session from list
//                                try {
//                                    session.close(CloseStatus.SERVER_ERROR);
//                                } catch (IOException ex) {
//                                    log.warn("can't close session: {}", session, ex);
//                                }
//                                unregisterSession(session);
//                                return session;
//                            }
//                        });
//                    }
//                });
//                CompletableFuture.supplyAsync(() -> {
//                            try {
////                                log.info("sendAll broadcast to: {}", session.getId());
//                                session.sendMessage(message);
//                                return session;
//                            } catch (IOException e) {
//                                log.warn("send - error", e);
//                                // TODO remove session from list
//                                try {
//                                    session.close(CloseStatus.SERVER_ERROR);
//                                } catch (IOException ex) {
//                                    log.warn("can't close session: {}", session, ex);
//                                }
//                                unregisterSession(session);
//                                return session;
//                            }
//                        }, executorService)
//        ).toList();
//        return CompletableFuture.allOf();
//        return CompletableFuture.allOf(all.toArray(new CompletableFuture[]{}));// TODO OOM
    }


    private byte[] toStringValue(Object payload) {
        try {
            return mapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
