package com.example.demo.websocket;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;

import java.io.IOException;

public class MonitoredWebSocketSession extends WebSocketSessionDecorator {

    private final WebSocketSessionMetrics metrics;

    public MonitoredWebSocketSession(WebSocketSessionMetrics metrics,
            WebSocketSession delegate) {
        super(delegate);
        this.metrics = metrics;
    }

    @Override
    public void sendMessage(@NonNull WebSocketMessage<?> message) throws IOException {
        String username = (String) getDelegate().getAttributes().get("username");
        try {
            metrics.startCount(username);
            super.sendMessage(message);
        } finally {
            metrics.finishCount(username);
        }
    }
}
