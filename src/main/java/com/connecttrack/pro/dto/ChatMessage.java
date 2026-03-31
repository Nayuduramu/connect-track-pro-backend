// File: src/main/java/com/connecttrack/pro/dto/ChatMessage.java
package com.connecttrack.pro.dto;

import java.time.LocalDateTime;

public class ChatMessage {
    private Long id;
    private String content;
    private String sender;
    private MessageType type;
    private String senderRole;
    private LocalDateTime timestamp; // New field for message time

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
