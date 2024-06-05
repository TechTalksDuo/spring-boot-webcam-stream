package com.example.demo.websocket;

import com.example.demo.ClientModeConfig;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.function.Consumer;

public class ExistOnCloseWebSocketHandler extends SampleWebSocketHandler {

    public ExistOnCloseWebSocketHandler(Consumer<ClientModeConfig.MessageOnSession> onMessage) {
        super(onMessage);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus closeStatus)
            throws Exception {
        super.afterConnectionClosed(session, closeStatus);
        System.exit(-1);

    }

}
