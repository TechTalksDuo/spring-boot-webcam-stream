package com.example.demo.backpressure;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class BackpressureSamplingService {

    private static final Logger log = getLogger(BackpressureSamplingService.class);
    static final int QUEUE_SIZE = 5;
    static final int NUMBER_OF_THREADS = 5;
    private final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final Executor executor = new ThreadPoolExecutor(NUMBER_OF_THREADS, NUMBER_OF_THREADS, 0L, TimeUnit.MILLISECONDS, workQueue);
    //        private final Executor executor = Executors.newFixedThreadPool(5);
    private final Map<UUID, LocalDateTime> inProgress = new ConcurrentHashMap<>();
    private final BackpressureMetrics metrics;

    public BackpressureSamplingService(BackpressureMetrics metrics) {
        this.metrics = metrics;
    }


    public void sample(UUID id, Runnable runnable) {
        metrics.onRequest();
        log.trace("sample - user {}", id);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = inProgress.putIfAbsent(id, now.plusSeconds(2));
        boolean firstTimeSampleFromUser = expirationTime == null;
        boolean shouldTriggerProcessing = false;
        boolean waitUntilExpirationTime = firstTimeSampleFromUser ? shouldTriggerProcessing : expirationTime.isAfter(now);

        if (waitUntilExpirationTime) {
            log.trace("sample - skip user {} until expirationTime: {}", id, expirationTime);
            metrics.onDroppedRequest();
        } else {
            try {
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
            } catch (RejectedExecutionException e) {
                metrics.onRejectedRequest();
                log.trace("sample - skip user {} due to limit reached: {}/{}", id, workQueue.size(), QUEUE_SIZE);
            }
        }

    }

}