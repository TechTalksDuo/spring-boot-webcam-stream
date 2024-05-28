package com.example.demo.websocket;

import java.util.List;

public class Messages {

    public record UserConnectedMessage(MessageType type, String me, List<OnlineUser> onlineUsers) {
        public UserConnectedMessage(String me, List<OnlineUser> onlineUsers) {
            this(MessageType.USER_CONNECTED, me, onlineUsers);
        }
    }
    public record OnlineUser(String username){}
    public record VideoMessage(MessageType type, String username, String videoStream){

        public VideoMessage(String username, String videoStream) {
            this(MessageType.VIDEO_FROM_USER, username, videoStream);
        }
    }
    public record VideoStoppedMessage(MessageType type, String username){

        public VideoStoppedMessage(String username) {
            this(MessageType.VIDEO_STOPPED, username);
        }
    }
    public record OnlineStatusChange(MessageType type, String username, Integer activeUsers){

        public OnlineStatusChange(String username, boolean online, Integer activeUsers) {
            this(online ? MessageType.USER_JOINED : MessageType.USER_LEFT, username, activeUsers);
        }
    }
    public record ContributionMessage(MessageType type, String videoStream){}
    public record VideoFeedbackMessage(MessageType type, String username, String description){
        public VideoFeedbackMessage(String username, String description) {
            this(MessageType.VIDEO_FEEDBACK, username, description);
        }
    }

    public enum MessageType {
        USER_CONNECTED,
        USER_JOINED,
        VIDEO_FROM_USER,
        VIDEO_STOPPED,
        VIDEO_FEEDBACK,
        USER_LEFT,

    }
}
