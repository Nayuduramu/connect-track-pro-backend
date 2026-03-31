// File: src/main/java/com/connecttrack/pro/entity/Notice.java
package com.connecttrack.pro.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notices")
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false; // Default: not pinned
}
