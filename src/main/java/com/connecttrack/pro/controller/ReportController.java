//src\main\java\com\connecttrack\pro\controller\ReportController.java
package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.DailyAttendanceReportDTO;
import com.connecttrack.pro.dto.PunctualityResponseDTO;
import com.connecttrack.pro.dto.TeamPresenceDTO;
import com.connecttrack.pro.dto.TimelineResponseDTO;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.repository.EmployeeRepository;
import com.connecttrack.pro.security.CustomUserDetails;
import com.connecttrack.pro.service.ReportGenerationService;
import com.connecttrack.pro.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    @Autowired
    private ReportGenerationService reportGenerationService;

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/download/all")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_SECTION_ADMIN') or (#employeeIds != null && #employeeIds.size() == 1 && #employeeIds.get(0) == authentication.principal.id)")
    public ResponseEntity<List<DailyAttendanceReportDTO>> downloadAllEmployeeReport(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "employeeIds", required = false) List<Long> employeeIds) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<DailyAttendanceReportDTO> reportData =
                reportGenerationService.generateFullReport(start, end, employeeIds);
        return ResponseEntity.ok(reportData);
    }

    @GetMapping("/download/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DailyAttendanceReportDTO>> downloadMyReport(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<DailyAttendanceReportDTO> reportData =
                reportGenerationService.generateReportForCurrentUser(start, end);
        return ResponseEntity.ok(reportData);
    }

    // --------------------------------------------------------------
    // UPDATED METHOD: Team Presence by Date
    // --------------------------------------------------------------
    @GetMapping("/team-presence")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_SECTION_ADMIN')")
    public ResponseEntity<List<TeamPresenceDTO>> getTeamPresence(
            @RequestParam("date") String dateString) {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee currentUser = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));

        LocalDate date = LocalDate.parse(dateString);

        List<TeamPresenceDTO> teamPresenceData = reportingService.getTeamPresence(currentUser, date);
        return ResponseEntity.ok(teamPresenceData);
    }
    // --------------------------------------------------------------

    @GetMapping("/timeline/{employeeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SECTION_ADMIN') or #employeeId == authentication.principal.id")
    public ResponseEntity<TimelineResponseDTO> getEmployeeTimelineForAdmin(
            @PathVariable Long employeeId,
            @RequestParam("date") String date) {

        TimelineResponseDTO timeline =
                reportingService.getEmployeeTimelineForDate(employeeId, LocalDate.parse(date));
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/punctuality/{employeeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SECTION_ADMIN') or #employeeId == authentication.principal.id")
    public ResponseEntity<PunctualityResponseDTO> getEmployeePunctualityForAdmin(
            @PathVariable Long employeeId,
            @RequestParam("thisWeek") boolean thisWeek) {

        PunctualityResponseDTO punctualityData =
                reportingService.getPunctualityStats(employeeId, thisWeek);
        return ResponseEntity.ok(punctualityData);
    }

    @GetMapping("/my-punctuality")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PunctualityResponseDTO> getMyPunctuality(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam("thisWeek") boolean thisWeek) {

        if (currentUser == null) {
            throw new RuntimeException("Authenticated user details not found.");
        }

        PunctualityResponseDTO punctualityData =
                reportingService.getPunctualityStats(currentUser.getId(), thisWeek);
        return ResponseEntity.ok(punctualityData);
    }
}
