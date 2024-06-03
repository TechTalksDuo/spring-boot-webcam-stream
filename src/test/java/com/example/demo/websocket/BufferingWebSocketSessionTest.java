package com.example.demo.websocket;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BufferingWebSocketSessionTest {

    @Test
    void testAllMessagesAreSent() throws IOException, InterruptedException {
        WebSocketSession delegate = mock(WebSocketSession.class);

        CountDownLatch latch = new CountDownLatch(2);
        doAnswer(ans -> {
            latch.countDown();
            return null;
        }).when(delegate).sendMessage(any());

        BufferingWebSocketSession buff = new BufferingWebSocketSession(delegate);

        TextMessage mesage1 = mock(TextMessage.class);
        TextMessage mesage2 = mock(TextMessage.class);

        buff.sendMessage(mesage1);
        buff.sendMessage(mesage2);

        latch.await();
        verify(delegate).sendMessage(mesage1);
        verify(delegate).sendMessage(mesage2);
    }
}
