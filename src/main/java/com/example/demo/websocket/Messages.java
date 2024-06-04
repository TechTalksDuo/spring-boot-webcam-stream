package com.example.demo.websocket;

import java.util.List;

public interface Messages {

    record UserConnectedMessage(MessageType type, String me, List<OnlineUser> onlineUsers) {
        public UserConnectedMessage(String me, List<OnlineUser> onlineUsers) {
            this(MessageType.USER_CONNECTED, me, onlineUsers);
        }
    }

    record OnlineUser(String username) {
    }

    record VideoMessage(MessageType type, String username, List<String> videoStream) {

        public VideoMessage(String username, List<String> videoStream) {
            this(MessageType.VIDEO_FROM_USER, username, videoStream);
        }
    }

    record VideoStoppedMessage(MessageType type, String username) {

        public VideoStoppedMessage(String username) {
            this(MessageType.VIDEO_STOPPED, username);
        }
    }

    record OnlineStatusChange(MessageType type, String username, Integer activeUsers) {

        public OnlineStatusChange(String username, boolean online, Integer activeUsers) {
            this(online ? MessageType.USER_JOINED : MessageType.USER_LEFT, username, activeUsers);
        }
    }

    record ContributionMessage(MessageType type, List<String> videoStream) {
    }

    record VideoFeedbackMessage(MessageType type, String username, Emotion emotion) {
        public VideoFeedbackMessage(String username, Emotion emotion) {
            this(MessageType.VIDEO_FEEDBACK, username, emotion);
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

    record Emotion(String label, String emoji, double score) {
    }
}
