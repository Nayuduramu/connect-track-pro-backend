package com.connecttrack.pro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class TimelinePeriodDTO {
    private String start;
    private String end;
    private String status; // "GREEN", "RED", "BLUE"
}