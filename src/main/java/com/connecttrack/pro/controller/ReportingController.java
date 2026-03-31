// File: src/main/java/com/connecttrack/pro/controller/ReportingController.java
package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.TimelineResponseDTO;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.repository.EmployeeRepository;
import com.connecttrack.pro.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee getCurrentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return employeeRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }

    // --- TIMELINE METHOD ---
    @GetMapping("/my-timeline")
    public ResponseEntity<TimelineResponseDTO> getMyTimeline(@RequestParam("date") String dateString) {
        Employee employee = getCurrentEmployee();
        LocalDate date = LocalDate.parse(dateString);
        return ResponseEntity.ok(reportingService.getEmployeeTimelineForDate(employee.getId(), date));
    }
}
