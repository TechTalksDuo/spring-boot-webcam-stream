package com.example.demo;

import com.example.demo.websocket.Messages;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@ConditionalOnProperty(name = "client.mode", havingValue = "true")
public class ClientModeConfig {
    private static final Logger log = getLogger(ClientModeConfig.class);

    @Autowired
    ObjectMapper mapper;

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
