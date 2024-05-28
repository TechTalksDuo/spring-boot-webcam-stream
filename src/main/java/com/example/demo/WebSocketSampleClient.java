package com.example.demo;


import com.example.demo.websocket.ExistOnCloseWebSocketHandler;
import com.example.demo.websocket.Messages;
import com.example.demo.websocket.SampleWebSocketHandler;
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
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@ConditionalOnProperty(name = "client.mode", havingValue = "true")
@Component
public class WebSocketSampleClient implements CommandLineRunner {
    private final WebSocketClient client;
    private final String[] availableImages;
    private final int rateMillis;
    private final int port;
    private final String host;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<ScheduledFuture<?>> scheduledFuture = new AtomicReference<>();

    public WebSocketSampleClient(WebSocketClient client, File[] availableImages,
                                 @Value("${websocket.rateMillis}") int rateMillis,
                                 @Value("${websocket.target.port}") int port,
                                 @Value("${websocket.target.host:localhost}") String host, ObjectMapper mapper
    ) {
        this.client = client;
        this.rateMillis = rateMillis;
        this.port = port;
        this.host = host;
        this.mapper = mapper;
        int LOOP_SIZE = 10;

        this.availableImages = new String[LOOP_SIZE];
        IntStream.range(0, LOOP_SIZE)
                .forEach(i -> {

                    var random = ThreadLocalRandom.current().nextInt(availableImages.length);
                    try {
                        this.availableImages[i] = "data:image/jpeg;base64," +
                                Base64.getEncoder().encodeToString(Files.readAllBytes(availableImages[random].toPath()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
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
                ), "ws://" + host + ":" + port + "/websocket")
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
        var random = ThreadLocalRandom.current().nextInt(availableImages.length);
        return availableImages[random];
    }

    byte[] toStringValue(Object payload) {
        try {
            return mapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
