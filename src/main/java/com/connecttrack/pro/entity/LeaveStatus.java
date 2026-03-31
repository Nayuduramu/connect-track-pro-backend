package com.connecttrack.pro.entity;

public enum LeaveStatus {
    PENDING,      // A new leave starts in this state
    APPROVED,     // Approved by approver/admin
    REJECTED,     // Rejected by approver
    CANCELLED,    // Cancelled by employee
    UTILIZED      // After leave is consumed
}
