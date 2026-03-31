package com.connecttrack.pro.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class GrantLeaveRequest {
    private Long employeeId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
    private String leaveType; // <-- ADD THIS LINE
}