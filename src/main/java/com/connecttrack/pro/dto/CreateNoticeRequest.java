package com.connecttrack.pro.dto;

import lombok.Data;

@Data
public class CreateNoticeRequest {
    private String title;
    private String content;
}