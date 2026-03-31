package com.connecttrack.pro.dto;
import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveRequestDTO {
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
    private String leaveType; // <-- Ensure this is a String
    private Long approverId;
}