package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "client.mode", havingValue = "true")
public class ClientModeConfig {
    @Bean
    List<File> availableImages(@Value("${websocket.input.dir}") String inputDir) throws FileNotFoundException {
        return Arrays.stream(ResourceUtils.getFile(inputDir).listFiles())
                .sorted()
                .toList();

    }

    @Bean
    WebSocketClient sampleClient() {
        return new StandardWebSocketClient();
    }

    public record MessageOnSession(WebSocketSession session, Object message) {
    }
}
