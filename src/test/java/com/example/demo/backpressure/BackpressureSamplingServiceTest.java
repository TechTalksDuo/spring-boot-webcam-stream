package com.example.demo.backpressure;


import com.example.demo.backpressure.BackpressureSamplingService;
import com.example.demo.client.OllamaClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {OllamaClient.class, BackpressureSamplingService.class},
        properties = "logging.level.com.example.demo=TRACE")
class BackpressureSamplingServiceTest {
    private static final Logger log = LoggerFactory.getLogger(BackpressureSamplingServiceTest.class);
    ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    BackpressureSamplingService service;

    @MockBean
    BackpressureMetrics metrics;

    @Mock
    Supplier supplier;

    @Test
    void testSecondCallSkippedWhenBeforeExpiration() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        var user = UUID.randomUUID();
        service.sample(user, () -> {
            System.out.println("try 1 ");
            latch.countDown();
        });
        service.sample(user, () -> {
            System.out.println("try 2 ");
            supplier.get();
        });
        latch.await();
        verify(supplier, never()).get();
        verify(metrics, times(2)).onRequest();
    }
    @Test
    void testNoSkipAfterExpirationHappens() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(2);
        var user = UUID.randomUUID();
        service.sample(user, () -> {
            System.out.println("try 1 ");
            latch.countDown();
        });
        Thread.sleep(2000L);
        service.sample(user, () -> {
            System.out.println("try 2 ");
            latch.countDown();
        });
        latch.await();
        verify(metrics, times(2)).onRequest();
    }

    @Test
    void testQueueAcceptsExtraRequests() throws InterruptedException {

        int TOTAL = 4;
        CountDownLatch latch = new CountDownLatch(TOTAL);
        IntStream.range(0, TOTAL)
                .forEach(i -> {
                    service.sample(UUID.randomUUID(), () -> {
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("try " + i);
                        latch.countDown();
                    });
                });
        latch.await();
        verify(metrics, times(TOTAL)).onRequest();
        verify(metrics, times(TOTAL)).onFinishedRequest();
    }

    @Test
    void testQueueRejectAfterLimit() throws InterruptedException {

        int REJECTED_REQUESTS = 10;
        int TOTAL = BackpressureSamplingService.NUMBER_OF_THREADS + BackpressureSamplingService.QUEUE_SIZE + REJECTED_REQUESTS;
        int EXPECTED_FINISHED_REQUESTS = TOTAL - REJECTED_REQUESTS;
        CountDownLatch latch = new CountDownLatch(EXPECTED_FINISHED_REQUESTS);
        IntStream.range(0, TOTAL)
                .forEach(i -> executorService.submit(() ->
                        service.sample(UUID.randomUUID(), () -> {
                            try {
                                Thread.sleep(500L);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            System.out.println("received response " + i);
                            latch.countDown();
                        })));
        latch.await();
        verify(metrics, times(TOTAL)).onRequest();
        verify(metrics, times(REJECTED_REQUESTS)).onRejectedRequest();
        verify(metrics, times(EXPECTED_FINISHED_REQUESTS)).onFinishedRequest();
    }

}