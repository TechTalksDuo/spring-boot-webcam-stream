package com.example.demo.websocket;

import com.example.demo.ClientModeConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.client.WebSocketClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import static org.slf4j.LoggerFactory.getLogger;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
        "client.mode=true",
        "websocket.input.dir=file://${PWD}/src/test/resources/input_small",
                "websocket.rateMillis=1000",
                "websocket.target.port=${server.port}",
        })
class WebSocketConfigTest {

    @LocalServerPort
    int port;
    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    WebSocketClient client;

    @Autowired
    File[] availableImages;

    @Test
    void test() throws IOException, InterruptedException {

        var random = ThreadLocalRandom.current().nextInt(availableImages.length);
        String image = Base64.getEncoder().encodeToString(Files.readAllBytes(availableImages[random].toPath()));

        client.execute(new SampleWebSocketHandler(message -> {
                    // TODO change rate
                }), "ws://localhost:" + port+ "/websocket")
                .thenApply(session -> {

                    try {
                        session.sendMessage(new TextMessage(toStringValue(
                                new Messages.ContributionMessage(Messages.MessageType.VIDEO_FROM_USER, image))));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
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
                })
                ;

        Thread.sleep(20000L);
    }

    private byte[] toStringValue(Object payload) {
        try {
            return mapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
