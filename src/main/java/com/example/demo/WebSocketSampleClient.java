package com.example.demo;


import com.example.demo.websocket.ExistOnCloseWebSocketHandler;
import com.example.demo.websocket.Messages;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@ConditionalOnProperty(name = "client.mode", havingValue = "true")
@Component
public class WebSocketSampleClient implements CommandLineRunner {
    private final WebSocketClient client;
    private final List<String> availableImages;
    private int currentIndex;
    private final int rateMillis;
    private final int port;
    private final String host;
    private final String protocol;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<ScheduledFuture<?>> scheduledFuture = new AtomicReference<>();

    public WebSocketSampleClient(WebSocketClient client, List<File> availableImages,
                                 @Value("${websocket.protocol:ws}") String protocol,
                                 @Value("${websocket.rateMillis}") int rateMillis,
                                 @Value("${websocket.target.port}") int port,
                                 @Value("${websocket.target.host:localhost}") String host, ObjectMapper mapper
    ) {
        this.client = client;
        this.protocol = protocol;
        this.rateMillis = rateMillis;
        this.port = port;
        this.host = host;
        this.mapper = mapper;

        this.availableImages = availableImages
                .stream()
                .map(frame -> {
                    try {
                        return "data:image/jpeg;base64," +
                                Base64.getEncoder().encodeToString(Files.readAllBytes(frame.toPath()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        currentIndex = 0;

    }

    @Override
    public void run(String... args) throws Exception {

        client.execute(new ExistOnCloseWebSocketHandler(
                        message -> {
                            // TODO change rate
//                            if (message.equals("something") && scheduledFuture.get() != null) {
//                                scheduledFuture.get().cancel(true);
//                                scheduledFuture.set(scheduledService.scheduleAtFixedRate(sendImageToSession(message.session()), 0, 500, TimeUnit.MILLISECONDS));
//                            }
                        }
                ), protocol + "://" + host + ":" + port + "/websocket")
                .thenApply(session -> {
                    scheduledFuture.set(scheduledService.scheduleAtFixedRate(sendImageToSession(session), 0, rateMillis, TimeUnit.MILLISECONDS));
                    return session;
                })
                .handle((session, ex) -> {
                    if (ex != null) {
                        try {
                            session.close(CloseStatus.SERVER_ERROR);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return null;
                });
    }

    Runnable sendImageToSession(WebSocketSession session) {
        return () -> {
            try {
                String image = getImage();
                session.sendMessage(new TextMessage(toStringValue(
                        new Messages.ContributionMessage(Messages.MessageType.VIDEO_FROM_USER, image))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        };
    }

    private String getImage() throws IOException {
        return availableImages.get(currentIndex++ % availableImages.size());
    }

    byte[] toStringValue(Object payload) {
        try {
            return mapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
