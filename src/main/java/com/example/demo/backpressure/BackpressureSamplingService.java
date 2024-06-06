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
    static final int QUEUE_SIZE = 64;
    static final int NUMBER_OF_THREADS = 32;
    private final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final Executor executor = new ThreadPoolExecutor(NUMBER_OF_THREADS, NUMBER_OF_THREADS, 0L, TimeUnit.MILLISECONDS, workQueue);
    //        private final Executor executor = Executors.newFixedThreadPool(5);
    private boolean isEnabled = true;
    private final Executor noBackpressure = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<UUID, LocalDateTime> inProgress = new ConcurrentHashMap<>();

    public void sample(UUID id, Runnable runnable) {
        if (!isEnabled) {
            try {
                noBackpressure.execute(() -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        log.trace("sample - user {}", id);

        LocalDateTime now = LocalDateTime.now();
        inProgress.compute(id, (k, v) -> {
            if (v == null || now.isAfter(v)) {
                callMethod(id, runnable);
                return now.plusSeconds(2);
            }
            log.debug("sample - skip user {} until expirationTime: {}", id, v);
            return v;
        });

    }

    private void callMethod(UUID id, Runnable runnable) {
        try {
            executor.execute(() -> {
                log.debug("sample - starting user {} workQueue: {}/{}", id, workQueue.size(), QUEUE_SIZE);
                try {
                    runnable.run();
                    log.debug("sample - finished user {} workQueue: {}/{}", id, workQueue.size(), QUEUE_SIZE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            log.debug("sample - skip user {} due to limit reached: {}/{}", id, workQueue.size(), QUEUE_SIZE);
        }
    }

}
