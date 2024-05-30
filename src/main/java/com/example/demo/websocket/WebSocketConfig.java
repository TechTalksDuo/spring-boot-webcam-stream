package com.example.demo.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.slf4j.LoggerFactory.getLogger;

@ConditionalOnProperty(name = "client.mode", matchIfMissing = true, havingValue = "false")
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private static final Logger log = getLogger(WebSocketConfig.class);

    @Autowired
    ObjectMapper mapper;
    @Autowired
    BroadcastService broadcastService;
    @Autowired
    WebSocketMetrics metrics;


    @Bean
    public List<String> usernames(@Value("${usernames.path:file:///home/www/assets/username-list.md}") String usernamesPath) {
        try {
            String  usernameContent = Files
                    .readString(ResourceUtils.getFile(usernamesPath).toPath());
            String[] usernameList = usernameContent.split("\n");
            return Arrays.stream(usernameList).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(usernames(null)), "/websocket")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    class WebSocketHandler extends AbstractWebSocketHandler {
        private final List<String> usernames;

        WebSocketHandler(List<String> usernames) {
            this.usernames = usernames;
        }

        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {

            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Binary messages not supported"));
            } catch (IOException ex) {
                // ignore
            }
        }
        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            metrics.onMessage();
// Handle incoming messages here
////            TODO process async
//            executorService.submit(() -> {
//
//                String receivedMessage = (String) message.getPayload();
//// Process the message and send a response if needed`
//                try {
//                    Messages.ContributionMessage contribution = mapper.readValue(receivedMessage, Messages.ContributionMessage.class);
//                    broadcastService.send(session, contribution)
//                            .join();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//
            String receivedMessage = (String) message.getPayload();
// Process the message and send a response if needed`
            try {
                Messages.ContributionMessage contribution = mapper.readValue(receivedMessage, Messages.ContributionMessage.class);
                broadcastService.send(session, contribution);
            } catch (IOException e) {
                log.warn("can't process message", e);
            }
        }
        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            metrics.onNewSession();
// Perform actions when a new WebSocket connection is established
            try {
                session.setBinaryMessageSizeLimit(2 * 1024 * 1024);
                session.setTextMessageSizeLimit(2 * 1024 * 1024);
//                TODO decorator session?
                ConcurrentWebSocketSessionDecorator decorator = new ConcurrentWebSocketSessionDecorator(session, 500, 5 * 1024 * 1024,
                        ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP);
                String principal = usernames.get(ThreadLocalRandom.current().nextInt(usernames.size()));
                session.getAttributes().put("username", principal);
                session.getAttributes().put("id", UUID.randomUUID());

                List<Messages.OnlineUser> onlineUsers = broadcastService.registerSession(decorator);

                decorator.sendMessage(new TextMessage(toStringValue(
                        new Messages.UserConnectedMessage(principal, onlineUsers))));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private byte[] toStringValue(Object payload) throws JsonProcessingException {
            return mapper.writeValueAsBytes(payload);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            metrics.onClosedSession();
            broadcastService.unregisterSession(session);
// Perform actions when a WebSocket connection is closed
        }
    }
}
