package com.connecttrack.pro.dto; // <-- THIS LINE FIXES THE ERROR

import lombok.Data;

@Data
public class AttendanceEventRequest {
    private String eventType;
    private String routerMac;
    private String timestamp; 
}