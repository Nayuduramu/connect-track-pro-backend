package com.connecttrack.pro.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoticeDTO {
    private Long id;
    private String title;
    private String content;
    private String author;
    private LocalDateTime date;
    private boolean isPinned;
}