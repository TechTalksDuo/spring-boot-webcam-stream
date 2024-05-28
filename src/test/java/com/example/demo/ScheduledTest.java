package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ScheduledTest {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTest.class);

    @Test
    void test1() throws ExecutionException, InterruptedException {
        ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();

        ScheduledFuture<?> running = scheduledService.scheduleAtFixedRate(() -> log.info("running"), 0, 1, TimeUnit.SECONDS);

        running.get();
    }
    @Test
    void test2() throws ExecutionException, InterruptedException {
        ScheduledThreadPoolExecutor scheduledService = new ScheduledThreadPoolExecutor(1);
        scheduledService.setRemoveOnCancelPolicy(true);
//        ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();

        CountDownLatch latch = new CountDownLatch(5);
        ScheduledFuture<?> running = scheduledService.scheduleAtFixedRate(() -> {
            log.info("running size: {}", scheduledService.getQueue().size());
            latch.countDown();
        }, 0, 1, TimeUnit.SECONDS);

        latch.await();
        running.cancel(true);

        var latchFaster = new CountDownLatch(10);
        running = scheduledService.scheduleAtFixedRate(() -> {
            log.info("running faster {}", scheduledService.getQueue().size());
            latchFaster.countDown();
        }, 0, 500, TimeUnit.MILLISECONDS);
        latchFaster.await();
    }
}
