package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.AttendanceEventRequest;
import com.connecttrack.pro.service.AttendanceService; // <-- NEW IMPORT
import lombok.RequiredArgsConstructor; // <-- NEW IMPORT
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor // <-- NEW ANNOTATION
public class AttendanceController {

    // --- REFACTORED DEPENDENCIES ---
    private final AttendanceService attendanceService; // <-- USE THE SERVICE

    @PostMapping("/events")
    public ResponseEntity<?> logAttendanceEvent(@RequestBody AttendanceEventRequest request) {
        // The controller's job is simple: delegate to the service.
        // All logic, security, and transaction handling is now in AttendanceService.
        try {
            attendanceService.logAttendanceEvent(request);
            return ResponseEntity.ok("Event logged successfully.");
        } catch (Exception e) {
            // If the service throws an exception, it will now be caught here
            // and a proper error response will be sent to the app.
            return ResponseEntity.internalServerError().body("Failed to log event: " + e.getMessage());
        }
    }
}