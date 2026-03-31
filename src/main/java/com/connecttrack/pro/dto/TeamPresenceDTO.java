package com.connecttrack.pro.dto;

import lombok.Data;
import java.time.LocalTime;
import java.util.List;

@Data
public class TeamPresenceDTO {
    private Long employeeId;
    private String employeeName;
    private String status;
    private LocalTime effectiveStartTime;
    private LocalTime effectiveEndTime;
    private LocalTime firstCheckIn; // <-- ADD THIS FIELD
    private List<TimelinePeriodDTO> timelinePeriods;
}