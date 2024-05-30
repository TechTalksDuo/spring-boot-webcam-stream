package com.example.demo.websocket;

import com.example.demo.ClientModeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.function.Consumer;

public class ExistOnCloseWebSocketHandler extends SampleWebSocketHandler {

    public ExistOnCloseWebSocketHandler(Consumer<ClientModeConfig.MessageOnSession> onMessage) {
        super(onMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        super.afterConnectionClosed(session, closeStatus);
        System.exit(-1);

    }

}
