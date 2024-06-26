package com.example.demo.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class WebSocketSessionMetrics {

    private final MeterRegistry meterRegistry;

    public WebSocketSessionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    void startCount(String sessionId) {
        Counter.builder("websocket.session.send.start.count")
                .tags("thread", Thread.currentThread().getName())
                .tags("sessionId", sessionId)
                .register(meterRegistry)
                .increment();
    }

    void finishCount(String sessionId) {
        Counter.builder("websocket.session.send.finish.count")
                .tags("thread", Thread.currentThread().getName())
                .tags("sessionId", sessionId)
                .register(meterRegistry)
                .increment();
    }
}
