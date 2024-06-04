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

    void startCount() {
        Counter.builder("websocket.session.send.start.count")
                .tags("thread", Thread.currentThread().getName())
                .register(meterRegistry)
                .increment();
    }

    void finishCount() {
        Counter.builder("websocket.session.send.finish.count")
                .tags("thread", Thread.currentThread().getName())
                .register(meterRegistry)
                .increment();
    }
}
