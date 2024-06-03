package com.example.demo.backpressure;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class BackpressureSamplingService {

    private static final Logger log = getLogger(BackpressureSamplingService.class);
    static final int QUEUE_SIZE = 5;
    static final int NUMBER_OF_THREADS = 10;
    private final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final Executor executor = new ThreadPoolExecutor(NUMBER_OF_THREADS, NUMBER_OF_THREADS, 0L, TimeUnit.MILLISECONDS, workQueue);
    //        private final Executor executor = Executors.newFixedThreadPool(5);
    private boolean isEnabled = true;
    private final Executor noBackpressure = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<UUID, LocalDateTime> inProgress = new ConcurrentHashMap<>();
    private final BackpressureMetrics metrics;

    public BackpressureSamplingService(BackpressureMetrics metrics) {
        this.metrics = metrics;
    }


    public void sample(UUID id, Runnable runnable) {
        if (!isEnabled) {
            metrics.onRequest();
            try {
                noBackpressure.execute(() -> {
                    try {
                        runnable.run();
                        metrics.onFinishedRequest();
                    } catch (Exception e) {
                        e.printStackTrace();
                        metrics.onError();
                    }
                });
            } catch (Exception e) {
                metrics.onRejectedRequest();
            }
            return;
        }
        metrics.onRequest();
        log.trace("sample - user {}", id);

        LocalDateTime now = LocalDateTime.now();
        inProgress.compute(id, (k, v) -> {
            if (v == null || now.isAfter(v)) {
                callMethod(id, runnable);
                return now.plusSeconds(2);
            }
            log.debug("sample - skip user {} until expirationTime: {}", id, v);
            metrics.onDroppedRequest();
            return v;
        });

    }

    private void callMethod(UUID id, Runnable runnable) {
        try {
            metrics.onSubmit();
            executor.execute(() -> {
                log.info("sample - starting user {} workQueue: {}/{}", id, workQueue.size(), QUEUE_SIZE);
                try {
                    runnable.run();
                    log.info("sample - finished user {} workQueue: {}/{}", id, workQueue.size(), QUEUE_SIZE);
                    metrics.onFinishedRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                    metrics.onError();
                }
            });
        } catch (Exception e) {
            metrics.onRejectedRequest();
            log.debug("sample - skip user {} due to limit reached: {}/{}", id, workQueue.size(), QUEUE_SIZE);
        }
    }

}
