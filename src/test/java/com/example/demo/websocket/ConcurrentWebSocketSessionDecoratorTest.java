package com.example.demo.websocket;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class ConcurrentWebSocketSessionDecoratorTest {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentWebSocketSessionDecoratorTest.class);

    ExecutorService receiveMessagesExecutor = Executors.newVirtualThreadPerTaskExecutor();
    ExecutorService sentToSessionsExecutor = Executors.newVirtualThreadPerTaskExecutor();
    ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);

    @Test
    void testSimple() throws IOException, InterruptedException {
        int NUMBER_OF_MESSAGES = 700;

        CountDownLatch latch = new CountDownLatch(NUMBER_OF_MESSAGES);
        WebSocketSession delegate = Mockito.mock(WebSocketSession.class);
        WebSocketMessage<?> message = Mockito.mock(WebSocketMessage.class);
        Mockito.doAnswer(ans -> {
            log.info("send on threadId: {}", Thread.currentThread());
            Thread.sleep(12);
            latch.countDown();
            return null;
        }).when(delegate).sendMessage(message);


        WebSocketSessionDecorator session = new WebSocketSessionDecorator(delegate);
//        WebSocketSessionDecorator session = new ConcurrentWebSocketSessionDecorator(delegate, 1000, 16*1024);

        IntStream.range(0, NUMBER_OF_MESSAGES)
                .forEach(currentMessage -> {
                    sentToSessionsExecutor.submit(() -> {
                        log.info("send currentMessage: {}, session: {}, thread: {}", currentMessage, session, Thread.currentThread().threadId());
                        try {
                            session.sendMessage(message);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });
        latch.await();
    }

    @Test
    void test() throws IOException, InterruptedException {
        int NUMBER_OF_SECONDS = 10;
        int NUMBER_OF_SESSIONS = 50;
        int NUMBER_OF_MESSAGES = 700;

        CountDownLatch latch = new CountDownLatch(NUMBER_OF_SECONDS * NUMBER_OF_MESSAGES * NUMBER_OF_SESSIONS);
        WebSocketSession delegate = Mockito.mock(WebSocketSession.class);
        WebSocketMessage<?> message = Mockito.mock(WebSocketMessage.class);
        Mockito.doAnswer(ans -> {
            log.info("send on threadId: {}", Thread.currentThread());
            Thread.sleep(12);
            latch.countDown();
            return null;
        }).when(delegate).sendMessage(message);


        List<WebSocketSessionDecorator> sessions = IntStream.range(0, NUMBER_OF_SESSIONS)
//                .mapToObj(i -> new ConcurrentWebSocketSessionDecorator(delegate, 1000, 16 * 1024, ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP))
                .mapToObj(i -> new WebSocketSessionDecorator(delegate))
                .toList();

        scheduledExecutorService.scheduleAtFixedRate(() -> {

                    IntStream.range(0, NUMBER_OF_MESSAGES)
                            .forEach(currentMessage -> {
                                receiveMessagesExecutor.submit(() -> {
                                    log.info("prepare currentMessage: {}, thread: {}", currentMessage, Thread.currentThread().threadId());
                                    sessions
                                            .forEach(currentSession -> {
                                                sentToSessionsExecutor.submit(() -> {
                                                    log.info("send currentMessage: {}, session: {}, thread: {}", currentMessage, currentSession, Thread.currentThread().threadId());
                                                    try {
                                                        currentSession.sendMessage(message);
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                });
                                            });

                                });

                            });
                }
                , 0, 1, TimeUnit.SECONDS);

        latch.await();
    }
}
