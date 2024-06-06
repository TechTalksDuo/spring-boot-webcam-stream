package com.example.demo.websocket;

import com.example.demo.backpressure.BackpressureSamplingService;
import com.example.demo.client.LLMClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParseException;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.SessionLimitExceededException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BroadcastService {
    private static final String USERNAME = "username";
    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);
    private final ObjectMapper mapper;
    private final LLMClient client;
    private final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();
    private final BackpressureSamplingService samplingService;
    private final String prompt;
    private final boolean ollamaEnabled;

    @Autowired
    public BroadcastService(ObjectMapper mapper, LLMClient client, BackpressureSamplingService samplingService,
                            @Value("${ollama.prompt.file}") String promptPath,
                            @Value("${ollama.enabled}") boolean ollamaEnabled) throws IOException {
        this.mapper = mapper;
        this.client = client;
        this.samplingService = samplingService;
        this.ollamaEnabled = ollamaEnabled;
        this.prompt = Files.readString(ResourceUtils.getFile(promptPath).toPath());
        log.info("using prompt path: {}, content: {}", promptPath, prompt);
    }

    @Timed
    public List<Messages.OnlineUser> registerSession(WebSocketSession session) {
        allSessions.put(session.getId(), session);
        byte[] payload = toStringValue(new Messages.OnlineStatusChange((String) session.getAttributes().get(USERNAME),
                true, allSessions.size()));
        TextMessage message = new TextMessage(payload);
        sendAll(message, session);
        return getActiveUsers();
    }

    private List<Messages.OnlineUser> getActiveUsers() {
        return allSessions.entrySet()
                .stream()
                .map(s -> new Messages.OnlineUser((String) s.getValue().getAttributes().get(USERNAME)))
                .toList();
    }

    @Timed
    public void unregisterSession(WebSocketSession session) {
        allSessions.remove(session.getId());
        byte[] payload = toStringValue(new Messages.OnlineStatusChange(
                (String) session.getAttributes().get(USERNAME), false, allSessions.size()));
        TextMessage message = new TextMessage(payload);
        sendAll(message, session);
    }

    @Timed
    public CompletableFuture<Object> send(WebSocketSession senderSession, Messages.ContributionMessage contribution) {
        if (contribution.type() == Messages.MessageType.VIDEO_STOPPED) {

            Messages.VideoStoppedMessage update = new Messages.VideoStoppedMessage(
                    (String) senderSession.getAttributes().get(USERNAME));
            byte[] payload = toStringValue(update);
            TextMessage message = new TextMessage(payload);
            return sendAll(message, senderSession);
        }

        var id = (UUID) senderSession.getAttributes().get("id");
        if (ollamaEnabled) {
            samplingService.sample(id, () -> {
                String base64 = contribution.videoStream()
                        .substring(contribution.videoStream().lastIndexOf(",") + 1);
                var answer = client.ask(base64);

                log.debug("Got answer: {}", answer);
                Messages.VideoFeedbackMessage update = new Messages.VideoFeedbackMessage(
                        (String) senderSession.getAttributes().get(USERNAME),
                        answer);
                byte[] payload = toStringValue(update);
                TextMessage message = new TextMessage(payload);
                sendAll(message)
                        .join();
            });
        }

        Messages.VideoMessage update = new Messages.VideoMessage(
                (String) senderSession.getAttributes().get(USERNAME),
                contribution.videoStream());
        byte[] payload = toStringValue(update);
        TextMessage message = new TextMessage(payload);
        return sendAll(message, senderSession);
    }

    private CompletableFuture<Object> sendAll(TextMessage message) {
        return sendAll(message, null);
    }

    private CompletableFuture<Object> sendAll(TextMessage message, WebSocketSession senderSessionToSkip) {

        allSessions.forEach((k, v) -> {
            try {
                if (senderSessionToSkip == null || !senderSessionToSkip.getId().equals(v.getId())) {
                    if (v.isOpen()) {
                        try {
                            v.sendMessage(message);
                        } catch (IOException e) {
                            log.warn("sendAll - failed to send message to sessionId: {}", v.getId(), e);
                        }
                    }
                    else
                        unregisterSession(v);
                }
            } catch (SessionLimitExceededException e) {
                throw new MessageDeliveryException(e.getMessage());
            }
        });
        return CompletableFuture.completedFuture(null);
    }

    private byte[] toStringValue(Object payload) {
        try {
            return mapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new JsonParseException(e);
        }
    }
}
