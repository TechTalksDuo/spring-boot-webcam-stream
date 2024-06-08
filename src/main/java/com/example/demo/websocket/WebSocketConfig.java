package com.example.demo.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final List<String> usernames;
    private final AtomicInteger counter = new AtomicInteger();

    @Autowired
    public WebSocketConfig(ObjectMapper mapper, BroadcastService broadcastService, WebSocketMetrics metrics,
                           WebSocketSessionMetrics sessionMetrics,
                           List<String> usernames) {
        this.mapper = mapper;
        this.broadcastService = broadcastService;
        this.metrics = metrics;
        this.sessionMetrics = sessionMetrics;
        this.usernames = usernames;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(usernames), "/websocket")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    class WebSocketHandler extends AbstractWebSocketHandler {
        private final List<String> usernames;

        WebSocketHandler(List<String> usernames) {
            this.usernames = usernames;
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

                WebSocketSessionDecorator decorator = new MonitoredWebSocketSession(sessionMetrics, session);
                var buffered = new BufferingWebSocketSession(decorator);
                String principal = usernames.get(counter.incrementAndGet() % usernames.size());
                session.getAttributes().put("username", principal);
                session.getAttributes().put("id", UUID.randomUUID());

                List<Messages.OnlineUser> onlineUsers = broadcastService.registerSession(buffered);
                var timedTextMessage = new BufferingWebSocketSession.TimedTextMessage(new TextMessage(toStringValue(
                        new Messages.UserConnectedMessage(principal, onlineUsers))), LocalDateTime.now());

                buffered.sendMessage(timedTextMessage);

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
