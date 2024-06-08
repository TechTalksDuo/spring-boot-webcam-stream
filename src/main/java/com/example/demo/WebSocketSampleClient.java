package com.example.demo;

import com.example.demo.websocket.ExistOnCloseWebSocketHandler;
import com.example.demo.websocket.Messages;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.json.JsonParseException;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.slf4j.LoggerFactory.getLogger;

@ConditionalOnProperty(name = "client.mode", havingValue = "true")
@Component
public class WebSocketSampleClient implements CommandLineRunner {
    private static final Logger log = getLogger(WebSocketSampleClient.class);
    private final WebSocketClient client;
    private final List<String> availableImages;
    private final int rateMillis;
    private final ExecutorService executorService;
    private final int connections;
    private final int port;
    private final String host;
    private final String protocol;
    private final ObjectMapper mapper;

    public WebSocketSampleClient(WebSocketClient client, List<File> availableImages,
                                 @Value("${websocket.protocol:ws}") String protocol,
                                 @Value("${websocket.rateMillis}") int rateMillis,
                                 @Value("${websocket.target.connections:1}") int connections,
                                 @Value("${websocket.target.port}") int port,
                                 @Value("${websocket.target.host:localhost}") String host, ObjectMapper mapper) {
        this.client = client;
        this.protocol = protocol;
        this.rateMillis = rateMillis;
        this.connections = connections;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
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
                        throw new InvalidPathException(frame.getName(), e.getMessage());
                    }
                })
                .toList();

    }

    @Override
    public void run(String... args) throws Exception {

        if (connections == 1) {
            this.loop(0).run();
        } else {
            IntStream.range(0, connections)
                    .forEach(i -> {
                        log.info("run: connection: {}/{}", i, connections);
                        executorService.submit(loop(i));
                    });
        }
    }

    Runnable loop(int index) {
        String target = protocol + "://" + host + ":" + port + "/websocket";

        return () -> {
            try {
                log.info("loop: client-{}@{}", index, target);
                int currentIndex = 0;
                WebSocketSession session = client.execute(new ExistOnCloseWebSocketHandler(
                        message -> {

                        }), target).get();

                while (true) {
                    if (!session.isOpen())
                        break;

                    try {
                        String image = availableImages.get(currentIndex++ % availableImages.size());
                        session.sendMessage(new TextMessage(toStringValue(
                                new Messages.ContributionMessage(Messages.MessageType.VIDEO_FROM_USER, image))));
                    } catch (IOException e) {
                        throw new MessageDeliveryException(e.getMessage());
                    }
                    Thread.sleep(rateMillis);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Can't connect to: {}", target);
            }
        };
    }

    byte[] toStringValue(Object payload) {
        try {
            return mapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new JsonParseException(e);
        }
    }
}
