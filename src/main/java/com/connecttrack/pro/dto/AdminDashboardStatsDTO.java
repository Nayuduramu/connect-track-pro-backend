package com.connecttrack.pro.dto;

import lombok.Data;

@Data
public class AdminDashboardStatsDTO {
    // New fields to match the UI design
    private long totalStaff; // Formerly activeCount, now represents total manageable staff
    private long inactiveCount;
    private long connectedCount;
    private long notConnectedCount;
    private long todayLateCount;
    private long todayOnLeaveCount; // Renamed for clarity
}