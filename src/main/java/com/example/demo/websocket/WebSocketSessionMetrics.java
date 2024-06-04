package com.example.demo.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class WebSocketSessionMetrics {

    private final Counter startCount;
    private final Counter finishCount;

    public WebSocketSessionMetrics(MeterRegistry meterRegistry) {
        startCount = Counter.builder("websocket.session.send.start.count")
                .register(meterRegistry);
        finishCount = Counter.builder("websocket.session.send.finish.count")
                .register(meterRegistry);
    }

    void startCount() {
        startCount.increment();
    }

    void finishCount() {
        finishCount.increment();
    }
}
