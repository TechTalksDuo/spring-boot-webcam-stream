package com.example.demo.websocket;

import org.slf4j.Logger;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import static org.slf4j.LoggerFactory.getLogger;

public class BufferingWebSocketSession {
    private static final Logger log = getLogger(BufferingWebSocketSession.class);
    private final WebSocketSession delegate;
    private final LinkedBlockingQueue<TimedTextMessage> buffer = new LinkedBlockingQueue<>();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(64);
//    private static final ExecutorService executorService = Executors.newThreadPerTaskExecutor(
//            Thread.ofVirtual()
//                    .name("per-session-virtual-threads")
//                    .factory()
//    );
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
                    log.warn("send failed - session: {}, error: {}", delegate.getId(), e);
//                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void cancel() {
        future.cancel(true);
    }

    public record TimedTextMessage(TextMessage message, LocalDateTime timestamp){}
}
