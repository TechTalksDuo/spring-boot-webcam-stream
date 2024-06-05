package com.example.demo.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BufferingWebSocketSessionTest {

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
}
