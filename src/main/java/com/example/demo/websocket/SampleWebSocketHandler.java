package com.example.demo.websocket;

import com.example.demo.ClientModeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.function.Consumer;

public class SampleWebSocketHandler implements WebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(SampleWebSocketHandler.class);

    private final Consumer<ClientModeConfig.MessageOnSession> onMessage;

    public SampleWebSocketHandler(Consumer<ClientModeConfig.MessageOnSession> onMessage) {
        this.onMessage = onMessage;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        log.info("afterConnectionEstablished");
        session.setBinaryMessageSizeLimit(20 * 1024 * 1024);
        session.setTextMessageSizeLimit(20 * 1024 * 1024);
    }

    @Override
    public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message)
            throws Exception {
        log.trace("handleMessage {}", message.getPayload());
        onMessage.accept(new ClientModeConfig.MessageOnSession(session, message.getPayload()));
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("handleTransportError", exception);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus closeStatus)
            throws Exception {
        log.warn("afterConnectionClosed {}", closeStatus);

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

}
