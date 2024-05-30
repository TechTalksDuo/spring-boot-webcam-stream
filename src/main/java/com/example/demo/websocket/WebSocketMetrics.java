package com.example.demo.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class WebSocketMetrics {

    private final Counter newSessionCounter;
    private final Counter messagesCounter;
    private final Counter closedSessionCounter;
    public WebSocketMetrics(MeterRegistry meterRegistry) {
        newSessionCounter = Counter.builder("websocket.new.session.count")
                .register(meterRegistry);
        closedSessionCounter = Counter.builder("websocket.closed.session.count")
                .register(meterRegistry);
        messagesCounter = Counter.builder("websocket.messages.count")
                .register(meterRegistry);
    }

    void onNewSession() {
        newSessionCounter.increment();
    }

    void onClosedSession() {
        closedSessionCounter.increment();
    }
    void onMessage() {
        messagesCounter.increment();
    }
}
