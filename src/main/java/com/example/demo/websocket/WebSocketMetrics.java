package com.example.demo.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class WebSocketMetrics {

    private final Counter newSessionCounter;
    private final Counter closedSessionCounter;
    private final MeterRegistry meterRegistry;

    public WebSocketMetrics(MeterRegistry meterRegistry, MeterRegistry meterRegistry1) {
        newSessionCounter = Counter.builder("websocket.new.session.count")
                .register(meterRegistry);
        closedSessionCounter = Counter.builder("websocket.closed.session.count")
                .register(meterRegistry);
        this.meterRegistry = meterRegistry1;
    }

    void onNewSession() {
        newSessionCounter.increment();
    }

    void onClosedSession() {
        closedSessionCounter.increment();
    }
    void onMessage(String principal) {
        Counter.builder("websocket.messages.count")
                .tags("sessionId", principal)
                .register(meterRegistry)
                .increment();
    }
}
