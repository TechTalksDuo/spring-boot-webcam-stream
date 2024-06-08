package com.example.demo.websocket;

import org.springframework.messaging.MessageDeliveryException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class BufferingWebSocketSession {
    private final WebSocketSession delegate;
    private final LinkedBlockingQueue<TimedTextMessage> buffer = new LinkedBlockingQueue<>();
    private static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private Future<?> future;

    public BufferingWebSocketSession(WebSocketSession delegate) {
        this.delegate = delegate;
        waitToSend();
    }

    public WebSocketSession getDelegate() {
        return delegate;
    }

    public void sendMessage(TimedTextMessage message) {
        buffer.add(message);
    }

    void waitToSend() {
        if (future != null && !future.isCancelled()) {
            cancel();
        }
        future = executorService.submit(() -> {
            while (true) {
                TimedTextMessage message = buffer.take();
                if (message == null || message.timestamp().isBefore(LocalDateTime.now().minusSeconds(2))) {
                    continue;
                }
                try {
                    delegate.sendMessage(message.message());
                } catch (IOException e) {
                    throw new MessageDeliveryException("send failed - session: %s, error: %s".formatted(
                            delegate.getId(), e));
                }
            }
        });
    }

    public void cancel() {
        future.cancel(true);
    }

    public record TimedTextMessage(TextMessage message, LocalDateTime timestamp) {
    }
}
