package com.connecttrack.pro.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveDTO {
    private Long id;
    private String employeeName;
    
    // --- UPDATED FIELDS ---
    private String leaveTypeName;  // We will send the name of the leave type as a String
    private LocalDate fromDate;    // Changed from single 'leaveDate' to a start date
    private LocalDate toDate;      // Added an end date for the range
    private String reason;
    private String status;         // e.g. "PENDING", "APPROVED", "REJECTED"
    
    // The 'durationHours' field has been removed as it is now calculated by the date range.
}