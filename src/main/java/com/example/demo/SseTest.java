package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@RestController
public class SseTest {
    private ExecutorService nonBlockingService = Executors.newVirtualThreadPerTaskExecutor();

    @GetMapping("/sse")
    public SseEmitter handleSse() {
        SseEmitter emitter = new SseEmitter();
        nonBlockingService.execute(() -> {
            IntStream.range(0, 500)
                    .forEach(i -> {
                        try {
                            emitter.send("/sse" + " @ " + new Date());
                            Thread.sleep(1000L);
                        } catch (Exception ex) {
                            emitter.completeWithError(ex);
                        }
                    });
            // we could send more events
            emitter.complete();
        });
        return emitter;
    }
}
