package com.connecttrack.pro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApproverDTO {
    private Long id;
    private String name;
    private String role;
}