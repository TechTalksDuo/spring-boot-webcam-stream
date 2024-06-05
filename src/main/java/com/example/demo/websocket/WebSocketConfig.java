package com.example.demo.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.util.ResourceUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.slf4j.LoggerFactory.getLogger;

@ConditionalOnProperty(name = "client.mode", matchIfMissing = true, havingValue = "false")
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private static final Logger log = getLogger(WebSocketConfig.class);

    private final ObjectMapper mapper;
    private final BroadcastService broadcastService;
    private final WebSocketMetrics metrics;
    private final WebSocketSessionMetrics sessionMetrics;

    @Autowired
    public WebSocketConfig(ObjectMapper mapper, BroadcastService broadcastService, WebSocketMetrics metrics,
            WebSocketSessionMetrics sessionMetrics) {
        this.mapper = mapper;
        this.broadcastService = broadcastService;
        this.metrics = metrics;
        this.sessionMetrics = sessionMetrics;
    }

    @Bean
    public List<String> usernames(
            @Value("${usernames.path:file:///home/www/assets/username-list.md}") String usernamesPath) {
        try {
            String usernameContent = Files
                    .readString(ResourceUtils.getFile(usernamesPath).toPath());
            String[] usernameList = usernameContent.split("\n");
            return Arrays.stream(usernameList).toList();
        } catch (IOException e) {
            Faker faker = new Faker();
            List<String> usernameList = new ArrayList<>();
            while (usernameList.size() < 150) {
                usernameList.add(faker.name().username());
            }
            return usernameList;
        }
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(usernames(null)), "/websocket")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    class WebSocketHandler extends AbstractWebSocketHandler {
        private final List<String> availableUsernames;
        private final List<String> usernames;

        WebSocketHandler(List<String> usernames) {
            this.usernames = usernames;
            this.availableUsernames = new CopyOnWriteArrayList<>(usernames);
        }

        @Override
        protected void handleBinaryMessage(@NonNull WebSocketSession session, @NonNull BinaryMessage message) {

            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Binary messages not supported"));
            } catch (IOException ex) {
                // ignore
            }
        }

        @Override
        public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message) {
            metrics.onMessage((String) session.getAttributes().get("username"));

            String receivedMessage = (String) message.getPayload();
            // Process the message and send a response if needed`
            try {
                Messages.ContributionMessage contribution = mapper.readValue(receivedMessage,
                        Messages.ContributionMessage.class);
                broadcastService.send(session, contribution);
            } catch (IOException e) {
                log.warn("can't process message", e);
            }
        }

        @Override
        public void afterConnectionEstablished(@NonNull WebSocketSession session) {
            metrics.onNewSession();
            // Perform actions when a new WebSocket connection is established
            try {
                session.setBinaryMessageSizeLimit(2 * 1024 * 1024);
                session.setTextMessageSizeLimit(2 * 1024 * 1024);

                ConcurrentWebSocketSessionDecorator decorator = new MonitoredWebSocketSession(sessionMetrics, session,
                        1_000, 24 * 1024,
                        ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP);
                String principal = availableUsernames
                        .get(ThreadLocalRandom.current().nextInt(availableUsernames.size()));
                session.getAttributes().put("username", principal);
                session.getAttributes().put("id", UUID.randomUUID());
                availableUsernames.remove(principal);

                List<Messages.OnlineUser> onlineUsers = broadcastService.registerSession(decorator);

                if (availableUsernames.isEmpty()) {
                    availableUsernames.addAll(usernames.stream().map(s -> s + "-" + onlineUsers.size()).toList());
                }
                decorator.sendMessage(new TextMessage(toStringValue(
                        new Messages.UserConnectedMessage(principal, onlineUsers))));

            } catch (IOException e) {
                throw new MessageDeliveryException(e.getMessage());
            }
        }

        private byte[] toStringValue(Object payload) throws JsonProcessingException {
            return mapper.writeValueAsBytes(payload);
        }

        @Override
        public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
            metrics.onClosedSession();
            broadcastService.unregisterSession(session);
            // Perform actions when a WebSocket connection is closed
        }
    }
}
