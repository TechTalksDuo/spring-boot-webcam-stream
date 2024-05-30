package com.example.demo.backpressure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class BackpressureMetrics {

    private final Counter requestCounter;
    private final Counter droppedCounter;
    private final Counter rejectedCounter;
    private final Counter finishedCounter;
    private final Counter errorCounter;
    private final Counter submitCounter;

    public BackpressureMetrics(MeterRegistry meterRegistry) {
        requestCounter = Counter.builder("long.running.task.request.count")
                .register(meterRegistry);
        submitCounter = Counter.builder("long.running.task.submit.count")
                .register(meterRegistry);
        droppedCounter = Counter.builder("long.running.task.drop.count")
                .register(meterRegistry);
        rejectedCounter = Counter.builder("long.running.task.rejected.count")
                .register(meterRegistry);
        finishedCounter = Counter.builder("long.running.task.finished.count")
                .register(meterRegistry);
        errorCounter = Counter.builder("long.running.task.error.count")
                .register(meterRegistry);
    }

    void onRequest() {
        requestCounter.increment();
    }
    void onSubmit() {
        submitCounter.increment();
    }

    void onRejectedRequest() {
        rejectedCounter.increment();
    }

    void onError() {
        errorCounter.increment();
    }

    void onFinishedRequest() {
        finishedCounter.increment();
    }

    void onDroppedRequest() {
        droppedCounter.increment();
    }

}
