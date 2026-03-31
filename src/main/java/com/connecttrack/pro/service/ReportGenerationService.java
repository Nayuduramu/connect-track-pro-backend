package com.connecttrack.pro.service;

import com.connecttrack.pro.dto.DailyAttendanceReportDTO;
import com.connecttrack.pro.dto.DailyAttendanceReportDTO.WifiSessionDTO;
import com.connecttrack.pro.dto.TeamPresenceDTO;
import com.connecttrack.pro.dto.TimelineResponseDTO;
import com.connecttrack.pro.dto.PunctualityResponseDTO;
import com.connecttrack.pro.entity.*;
import com.connecttrack.pro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final HolidayRepository holidayRepository;
    private final ReportingService reportingService;
    private final LeaveRepository leaveRepository;
    private final WiFiRouterRepository wiFiRouterRepository;

    // -----------------------------------------------------------------------
    // 1️⃣ GENERATE FULL REPORT (ADMIN)
    // -----------------------------------------------------------------------
    public List<DailyAttendanceReportDTO> generateFullReport(LocalDate startDate, LocalDate endDate, List<Long> employeeIds) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee currentUser = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Employee> employeesToReport;
        String currentUserRole = currentUser.getRole().getName();

        // --- Role-Based Employee Filtering ---
        if ("ROLE_SUPER_ADMIN".equals(currentUserRole)) {
            employeesToReport = (employeeIds != null && !employeeIds.isEmpty())
                    ? employeeRepository.findAllById(employeeIds)
                    : employeeRepository.findAll();
        } else if (("ROLE_ADMIN".equals(currentUserRole) || "ROLE_SECTION_ADMIN".equals(currentUserRole)) 
                   && currentUser.getDepartment() != null) {
            List<Employee> employeesInDept =
                    employeeRepository.findAllByDepartmentOrderByIdAsc(currentUser.getDepartment());
            if (employeeIds != null && !employeeIds.isEmpty()) {
                employeesToReport = employeesInDept.stream()
                        .filter(emp -> employeeIds.contains(emp.getId()))
                        .collect(Collectors.toList());
            } else {
                employeesToReport = employeesInDept;
            }
        } else {
            return Collections.emptyList();
        }

        // --- Generate Data ---
        List<DailyAttendanceReportDTO> fullReport = new ArrayList<>();
        for (Employee employee : employeesToReport) {
            fullReport.addAll(generateReportForEmployee(employee, startDate, endDate));
        }

        // --- SORTING: Date Ascending -> Check-In Time Ascending ---
        fullReport.sort((r1, r2) -> {
            // 1. Sort by Date
            int dateCompare = r1.getDate().compareTo(r2.getDate());
            if (dateCompare != 0) return dateCompare;

            // 2. Sort by Check-In (Nulls last)
            LocalTime t1 = r1.getFirstCheckIn();
            LocalTime t2 = r2.getFirstCheckIn();

            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;

            return t1.compareTo(t2);
        });

        return fullReport;
    }

    // -----------------------------------------------------------------------
    // 2️⃣ GENERATE MY REPORT (USER)
    // -----------------------------------------------------------------------
    public List<DailyAttendanceReportDTO> generateReportForCurrentUser(LocalDate startDate, LocalDate endDate) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return generateReportForEmployee(employee, startDate, endDate);
    }

    // -----------------------------------------------------------------------
    // 3️⃣ CORE REPORT LOGIC (PER EMPLOYEE)
    // -----------------------------------------------------------------------
    private List<DailyAttendanceReportDTO> generateReportForEmployee(Employee employee, LocalDate startDate, LocalDate endDate) {
        List<DailyAttendanceReportDTO> employeeReport = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyAttendanceReportDTO dayReport = new DailyAttendanceReportDTO();
            dayReport.setEmployeeName(employee.getFullName());
            dayReport.setDate(date);

            // 1. Determine Non-Working Status (Holiday/Weekend/Leave)
            String nonWorkingStatus = determineNonWorkingStatus(date, employee.getId());

            // 2. Fetch Events
            List<AttendanceEvent> events = attendanceEventRepository
                    .findByEmployeeIdAndEventTimestampBetweenOrderByEventTimestampAsc(
                            employee.getId(), date.atStartOfDay(), date.atTime(LocalTime.MAX)
                    );

            // 3. Process Logic
            if (events.isEmpty()) {
                // --- CASE A: No Data ---
                if (nonWorkingStatus != null) {
                    dayReport.setStatus(nonWorkingStatus); // "Weekend", "Holiday", "On Leave"
                } else {
                    dayReport.setStatus("Absent");
                }
                // Other fields (check-in, duration) remain null/0
                employeeReport.add(dayReport);
                continue;
            }

            // --- CASE B: Data Exists (User Worked) ---
            // Calculate sessions even if it's a weekend/holiday
            processEventsForDay(dayReport, events, employee);

            // 4. Set Status
            if (nonWorkingStatus != null) {
                // User worked on a non-working day. Keep the status (e.g. "Weekend")
                // but the report will now contain the check-in/out and duration data.
                dayReport.setStatus(nonWorkingStatus);
            } else {
                // Normal working day logic
                LocalTime officeStartTime = reportingService.getEffectiveTimingsForEmployee(employee).getStartTime();
                if (dayReport.getFirstCheckIn() != null && dayReport.getFirstCheckIn().isAfter(officeStartTime)) {
                    dayReport.setStatus("Late");
                } else {
                    dayReport.setStatus("Present");
                }
            }

            employeeReport.add(dayReport);
        }

        return employeeReport;
    }

    // -----------------------------------------------------------------------
    // 4️⃣ EVENT PROCESSING (STATE MACHINE)
    // -----------------------------------------------------------------------
    private void processEventsForDay(DailyAttendanceReportDTO dayReport, List<AttendanceEvent> allEvents, Employee employee) {
        if (allEvents == null || allEvents.isEmpty()) {
            return;
        }
        
        // Ensure sorting by timestamp
        allEvents.sort(Comparator.comparing(AttendanceEvent::getEventTimestamp));

        // --- Overall Check-In / Check-Out ---
        // Based on the absolute first and last event of any type (Wi-Fi or GPS)
        dayReport.setFirstCheckIn(allEvents.get(0).getEventTimestamp().toLocalTime());
        dayReport.setLastCheckOut(allEvents.get(allEvents.size() - 1).getEventTimestamp().toLocalTime());

        // --- Filter for Wi-Fi Events Only ---
        // We only calculate "Sessions" based on Wi-Fi. GPS check-ins don't create duration sessions.
        List<AttendanceEvent> wifiEvents = allEvents.stream()
            .filter(e -> e.getRouterMac() != null && !e.getRouterMac().toLowerCase().contains("gps"))
            .collect(Collectors.toList());

        List<WifiSessionDTO> sessions = new ArrayList<>();
        
        // Map to track open sessions: RouterMAC -> StartTime
        Map<String, LocalTime> openSessions = new HashMap<>();

        for (AttendanceEvent event : wifiEvents) {
            String routerMac = event.getRouterMac();
            LocalTime eventTime = event.getEventTimestamp().toLocalTime();

            if (event.getEventType() == AttendanceEvent.EventType.CONNECT) {
                // If already connected to this specific router, ignore duplicate CONNECT
                if (openSessions.containsKey(routerMac)) {
                    continue;
                }
                
                // If connected to a DIFFERENT router, close the old session first
                // (Assuming single active connection logic for mobile)
                if (!openSessions.isEmpty()) {
                    String previousMac = openSessions.keySet().iterator().next();
                    LocalTime connectTime = openSessions.remove(previousMac);
                    long duration = Duration.between(connectTime, eventTime).toMinutes();
                    if (duration > 0) {
                        sessions.add(new WifiSessionDTO(getWifiName(previousMac), connectTime, eventTime, duration));
                    }
                }

                // Start new session
                openSessions.put(routerMac, eventTime);

            } else if (event.getEventType() == AttendanceEvent.EventType.DISCONNECT) {
                // Only process DISCONNECT if we have a matching open session
                if (openSessions.containsKey(routerMac)) {
                    LocalTime connectTime = openSessions.remove(routerMac);
                    long duration = Duration.between(connectTime, eventTime).toMinutes();
                    // Allow 0-minute sessions if explicitly disconnected to show activity
                    if (duration >= 0) { 
                        sessions.add(new WifiSessionDTO(getWifiName(routerMac), connectTime, eventTime, duration));
                    }
                }
            }
        }

        // --- Close Remaining Open Sessions ---
        // If the user is still connected at the end of the list (no DISCONNECT event)
        if (!openSessions.isEmpty()) {
            LocalTime effectiveEndTime = reportingService.getEffectiveTimingsForEmployee(employee).getEndTime();
            LocalTime now = LocalTime.now();
            
            openSessions.forEach((mac, connectTime) -> {
                LocalTime endSessionTime;
                // If today and currently working hours, end at 'now'. Else end at office close.
                if (dayReport.getDate().isEqual(LocalDate.now()) && now.isBefore(effectiveEndTime)) {
                    endSessionTime = now;
                } else {
                    endSessionTime = effectiveEndTime;
                }
                
                if (endSessionTime.isAfter(connectTime)) {
                    long duration = Duration.between(connectTime, endSessionTime).toMinutes();
                    if (duration >= 0) {
                        sessions.add(new WifiSessionDTO(getWifiName(mac), connectTime, endSessionTime, duration));
                    }
                }
            });
        }
        
        // Sort sessions chronologically
        sessions.sort(Comparator.comparing(WifiSessionDTO::getConnectedAt));
        dayReport.setWifiSessions(sessions);

        // Calculate Total Duration
        long totalMinutes = sessions.stream()
                .mapToLong(WifiSessionDTO::getDurationMinutes)
                .sum();
        dayReport.setTotalDurationMinutes(totalMinutes);
    }

    // -----------------------------------------------------------------------
    // 5️⃣ HELPERS
    // -----------------------------------------------------------------------

    private String determineNonWorkingStatus(LocalDate date, Long employeeId) {
        // 1. Check Holidays / Overridden Weekends in DB
        Optional<Holiday> holidayOptional = holidayRepository.findByHolidayDate(date);
        if (holidayOptional.isPresent()) {
            Holiday holiday = holidayOptional.get();
            return holiday.getHolidayType() == Holiday.HolidayType.WEEKEND_OVERRIDE 
                   ? "Weekend" 
                   : "Holiday";
        }

        // 2. Check Leaves
        if (leaveRepository.existsByEmployeeIdAndDateAndStatusNot(employeeId, date, LeaveStatus.CANCELLED)) {
            return "On Leave";
        }

        // 3. If no entry in holidays table, it is a Working Day.
        // Note: We pre-populated the table with weekends, so if a weekend date isn't there, 
        // it means the admin deleted it to make it a working day.
        return null; 
    }

    private String getWifiName(String macAddress) {
        if (macAddress == null) return "Unknown";
        if (macAddress.toLowerCase().contains("gps")) return "GPS Check-In";
        return wiFiRouterRepository.findByMacAddressIgnoreCase(macAddress)
                .map(WiFiRouter::getSsid)
                .orElse("Unknown: " + macAddress);
    }
}