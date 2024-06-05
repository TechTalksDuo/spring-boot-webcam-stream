package com.example.demo.websocket;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BufferingWebSocketSessionTest {
    private static final Logger log = LoggerFactory.getLogger(BufferingWebSocketSessionTest.class);

    @Test
    void testAllMessagesAreSent() throws IOException, InterruptedException {
        WebSocketSession delegate = mock(WebSocketSession.class);

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(ans -> {
            latch.countDown();
            return null;
        }).when(delegate).sendMessage(any());

        BufferingWebSocketSession buff = new BufferingWebSocketSession(delegate);

        TextMessage mesage11 = mock(TextMessage.class);
        TextMessage mesage22 = mock(TextMessage.class);

        BufferingWebSocketSession.TimedTextMessage mesage1 = mock(BufferingWebSocketSession.TimedTextMessage.class);
        when(mesage1.message()).thenReturn(mesage11);
        when(mesage1.timestamp()).thenReturn(LocalDateTime.now().minusSeconds(5));
        BufferingWebSocketSession.TimedTextMessage mesage2 = mock(BufferingWebSocketSession.TimedTextMessage.class);
        when(mesage2.message()).thenReturn(mesage22);
        when(mesage2.timestamp()).thenReturn(LocalDateTime.now().minusSeconds(1));

        buff.sendMessage(mesage1);
        buff.sendMessage(mesage2);

        latch.await();
        verify(delegate, never()).sendMessage(mesage11);
        verify(delegate).sendMessage(mesage22);
    }

    ExecutorService receiveMessagesExecutor = Executors.newFixedThreadPool(4);
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);

    @Test
    void testTime() throws IOException, InterruptedException {
        int NUMBER_OF_SECONDS = 10;
        int NUMBER_OF_SESSIONS = 50;
        int NUMBER_OF_MESSAGES = 700;

        CountDownLatch latch = new CountDownLatch(NUMBER_OF_SECONDS * NUMBER_OF_MESSAGES * NUMBER_OF_SESSIONS);
        WebSocketSession delegate = Mockito.mock(WebSocketSession.class);
        Mockito.doAnswer(ans -> {
            log.info("send on threadId: {}", Thread.currentThread());
            Thread.sleep(12);
            latch.countDown();
            return null;
        }).when(delegate).sendMessage(any());


        List<BufferingWebSocketSession> sessions = IntStream.range(0, NUMBER_OF_SESSIONS)
                .mapToObj(i -> new BufferingWebSocketSession(delegate))
//                .mapToObj(i -> new WebSocketSessionDecorator(delegate))
                .toList();

        scheduledExecutorService.scheduleAtFixedRate(() -> {

        IntStream.range(0, NUMBER_OF_MESSAGES)
                .forEach(currentMessage -> {
                    receiveMessagesExecutor.submit(() -> {
                        log.info("prepare currentMessage: {}, thread: {}", currentMessage, Thread.currentThread());
                        TextMessage mesage11 = mock(TextMessage.class);
                        BufferingWebSocketSession.TimedTextMessage mesage1 = mock(BufferingWebSocketSession.TimedTextMessage.class);
                        when(mesage1.message()).thenReturn(mesage11);
                        when(mesage1.timestamp()).thenReturn(LocalDateTime.now().plusSeconds(100));
                        sessions
                                .forEach(currentSession -> {
//                                    sentToSessionsExecutor.submit(() -> {
                                        log.info("send currentMessage: {}, session: {}, thread: {}", currentMessage, currentSession, Thread.currentThread());
                                        currentSession.sendMessage(mesage1);
//                                    });
                                });

                    });

                });
        }
        , 0, 1, TimeUnit.SECONDS);

        latch.await();
    }
}
