// File: src/main/java/com/connecttrack/pro/entity/ChatMessage.java
package com.connecttrack.pro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderName;

    @Lob // Use @Lob for potentially long text content
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String type; // e.g., CHAT, JOIN, LEAVE

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee sender;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}