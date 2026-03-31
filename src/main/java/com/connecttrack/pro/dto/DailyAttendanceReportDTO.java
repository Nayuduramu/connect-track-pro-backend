package com.connecttrack.pro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single day's attendance report for an employee,
 * including check-in/out details and Wi-Fi or GPS session information.
 */
@Data
public class DailyAttendanceReportDTO {

    /** Employee name for the report row */
    private String employeeName;

    /** Date of attendance record */
    private LocalDate date;

    /** Status for the day (Present, Late, Absent, Holiday, On Leave, etc.) */
    private String status;

    /** First check-in time detected */
    private LocalTime firstCheckIn;

    /** Last check-out time detected */
    private LocalTime lastCheckOut;

    /** Total working duration in minutes */
    private long totalDurationMinutes;

    /** A list of Wi-Fi or GPS connection sessions during the day */
    private List<WifiSessionDTO> wifiSessions = new ArrayList<>();

    /**
     * Represents one Wi-Fi or GPS connection session for the day.
     * Each session includes connection, disconnection, and total duration.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WifiSessionDTO {
        private String wifiName;
        private LocalTime connectedAt;
        private LocalTime disconnectedAt;
        private long durationMinutes;
    }
}
