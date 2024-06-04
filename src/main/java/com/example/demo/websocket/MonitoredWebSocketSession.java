package com.example.demo.websocket;

import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;

public class MonitoredWebSocketSession extends ConcurrentWebSocketSessionDecorator {

    private final WebSocketSessionMetrics metrics;

    public MonitoredWebSocketSession(WebSocketSessionMetrics metrics,
                                     WebSocketSession delegate, int sendTimeLimit, int bufferSizeLimit, OverflowStrategy overflowStrategy) {
        super(delegate, sendTimeLimit, bufferSizeLimit, overflowStrategy);
        this.metrics = metrics;
    }

    @Override
    public void sendMessage(WebSocketMessage<?> message) throws IOException {
        try {
            metrics.startCount();
            super.sendMessage(message);
        } finally {
            metrics.finishCount();
        }
    }
}
