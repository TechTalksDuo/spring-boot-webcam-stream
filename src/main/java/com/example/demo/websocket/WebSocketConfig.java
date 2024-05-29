package com.example.demo.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(), "/websocket")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    class WebSocketHandler extends AbstractWebSocketHandler {
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
                Faker faker = new Faker();
                String principal = faker.name().fullName();
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
