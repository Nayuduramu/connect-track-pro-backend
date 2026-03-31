// src/main/java/com/connecttrack/pro/service/ReportingService.java
package com.connecttrack.pro.service;

import com.connecttrack.pro.dto.*;
import com.connecttrack.pro.entity.*;
import com.connecttrack.pro.repository.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    @Autowired private AttendanceEventRepository attendanceEventRepository;
    @Autowired private AppSettingRepository appSettingRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LeaveRepository leaveRepository;
    @Autowired private HolidayRepository holidayRepository;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // ============================================================
    // 1️⃣ EMPLOYEE TIMELINE FOR SPECIFIC DATE
    // ============================================================
    public TimelineResponseDTO getEmployeeTimelineForDate(Long employeeId, LocalDate date) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found with id: " + employeeId));

        OfficeTimings effectiveTimings = getEffectiveTimingsForEmployee(employee);
        LocalTime officeStartTime = effectiveTimings.getStartTime();
        LocalTime officeEndTime = effectiveTimings.getEndTime();

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<AttendanceEvent> events =
                attendanceEventRepository
                        .findByEmployeeIdAndEventTimestampBetweenOrderByEventTimestampAsc(
                                employeeId, startOfDay, endOfDay
                        );

        List<TimelinePeriodDTO> timeline = new ArrayList<>();
        LocalDateTime cursor = startOfDay;
        boolean isConnected = false;

        if (!events.isEmpty() &&
                events.get(0).getEventType() == AttendanceEvent.EventType.DISCONNECT) {
            isConnected = true;
        }

        for (AttendanceEvent event : events) {

            if (event.getEventTimestamp().isAfter(cursor)) {
                addPeriod(
                        timeline,
                        cursor.toLocalTime(),
                        event.getEventTimestamp().toLocalTime(),
                        isConnected,
                        officeStartTime,
                        officeEndTime
                );
            }

            isConnected = (event.getEventType() ==
                    AttendanceEvent.EventType.CONNECT);

            cursor = event.getEventTimestamp();
        }

        addPeriod(
                timeline,
                cursor.toLocalTime(),
                endOfDay.toLocalTime(),
                isConnected,
                officeStartTime,
                officeEndTime
        );

        return new TimelineResponseDTO(timeline, officeStartTime, officeEndTime);
    }

    // ============================================================
    // 2️⃣ HELPER — ADD PERIOD BLOCK
    // ============================================================
    private void addPeriod(
            List<TimelinePeriodDTO> timeline,
            LocalTime start,
            LocalTime end,
            boolean connected,
            LocalTime officeStartTime,
            LocalTime officeEndTime
    ) {

        if (start.equals(end) || start.isAfter(end)) return;

        String status = connected ? "GREEN" : "RED";

        if (connected) {
            if (end.isBefore(officeStartTime) || end.equals(officeStartTime)) {
                status = "BLUE";
            } else if (start.isAfter(officeEndTime) || start.equals(officeEndTime)) {
                status = "BLUE";
            } else if (start.isBefore(officeStartTime)
                    && end.isAfter(officeStartTime)) {

                addPeriod(timeline, start, officeStartTime,
                        true, officeStartTime, officeEndTime);

                addPeriod(timeline, officeStartTime, end,
                        true, officeStartTime, officeEndTime);

                return;

            } else if (start.isBefore(officeEndTime)
                    && end.isAfter(officeEndTime)) {

                addPeriod(timeline, start, officeEndTime,
                        true, officeStartTime, officeEndTime);

                addPeriod(timeline, officeEndTime, end,
                        true, officeStartTime, officeEndTime);

                return;
            }
        }

        timeline.add(
                new TimelinePeriodDTO(
                        start.format(TIME_FORMATTER),
                        end.format(TIME_FORMATTER),
                        status
                )
        );
    }

    // ============================================================
    // 3️⃣ PUNCTUALITY STATISTICS
    // ============================================================
    public PunctualityResponseDTO getPunctualityStats(
            Long employeeId,
            boolean thisWeek
    ) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found with id: " + employeeId));

        LocalTime officeStartTime =
                getEffectiveTimingsForEmployee(employee).getStartTime();

        LocalDate today = LocalDate.now();

        LocalDate startOfWeek = thisWeek
                ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                : today.minusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        List<PunctualityStatDTO> stats = new ArrayList<>();

        for (int i = 0; i < 7; i++) {

            LocalDate currentDate = startOfWeek.plusDays(i);
            if (currentDate.isAfter(today)) continue;

            List<AttendanceEvent> events =
                    attendanceEventRepository
                            .findByEmployeeIdAndEventTimestampBetweenOrderByEventTimestampAsc(
                                    employeeId,
                                    currentDate.atStartOfDay(),
                                    currentDate.atTime(LocalTime.MAX)
                            );

            LocalTime firstCheckIn = events.stream()
                    .filter(e ->
                            e.getEventType() ==
                                    AttendanceEvent.EventType.CONNECT)
                    .map(e -> e.getEventTimestamp().toLocalTime())
                    .findFirst()
                    .orElse(null);

            PunctualityStatDTO dayStat =
                    new PunctualityStatDTO(
                            currentDate.getDayOfWeek(),
                            firstCheckIn
                    );

            dayStat.setLate(
                    firstCheckIn != null &&
                            firstCheckIn.isAfter(officeStartTime)
            );

            stats.add(dayStat);
        }

        return new PunctualityResponseDTO(stats, officeStartTime);
    }

    // ============================================================
    // 4️⃣ EFFECTIVE OFFICE TIMINGS
    // ============================================================
    public OfficeTimings getEffectiveTimingsForEmployee(Employee employee) {

        if (employee.getCustomStartTime() != null &&
                employee.getCustomEndTime() != null) {

            return new OfficeTimings(
                    employee.getCustomStartTime(),
                    employee.getCustomEndTime()
            );
        }

        String startValue =
                appSettingRepository.findById("office_start_time")
                        .map(AppSetting::getSettingValue)
                        .orElse("10:00");

        String endValue =
                appSettingRepository.findById("office_end_time")
                        .map(AppSetting::getSettingValue)
                        .orElse("18:00");

        return new OfficeTimings(
                LocalTime.parse(startValue),
                LocalTime.parse(endValue)
        );
    }

    // ============================================================
    // 5️⃣ 🆕 EFFECTIVE OFFICE LOCATION
    // ============================================================
    public OfficeLocation getEffectiveLocationForEmployee(Employee employee) {

        // If employee has custom office location
        if (employee.getOfficeLatitude() != null &&
                employee.getOfficeLongitude() != null) {

            return new OfficeLocation(
                    employee.getOfficeLatitude(),
                    employee.getOfficeLongitude(),
                    employee.getOfficeRadius() != null
                            ? employee.getOfficeRadius()
                            : 100.0
            );
        }

        // Fallback to global settings
        String latStr =
                appSettingRepository.findById("office_latitude")
                        .map(AppSetting::getSettingValue)
                        .orElse("0.0");

        String lonStr =
                appSettingRepository.findById("office_longitude")
                        .map(AppSetting::getSettingValue)
                        .orElse("0.0");

        String radStr =
                appSettingRepository.findById("office_radius")
                        .map(AppSetting::getSettingValue)
                        .orElse("100.0");

        return new OfficeLocation(
                Double.parseDouble(latStr),
                Double.parseDouble(lonStr),
                Double.parseDouble(radStr)
        );
    }

    // ============================================================
    // 6️⃣ TEAM PRESENCE
    // ============================================================
    public List<TeamPresenceDTO> getTeamPresence(
            Employee currentUser,
            LocalDate date
    ) {

        List<Employee> employeesToDisplay;
        String userRole = currentUser.getRole().getName();

        if ("ROLE_SUPER_ADMIN".equals(userRole)) {
            employeesToDisplay =
                    employeeRepository.findAllByStatus(EmployeeStatus.ACTIVE);

        } else if (("ROLE_ADMIN".equals(userRole)
                || "ROLE_SECTION_ADMIN".equals(userRole))
                && currentUser.getDepartment() != null) {

            employeesToDisplay =
                    employeeRepository.findAllByDepartmentAndStatus(
                            currentUser.getDepartment(),
                            EmployeeStatus.ACTIVE
                    );

        } else {
            return Collections.emptyList();
        }

        return employeesToDisplay.stream().map(employee -> {

            TeamPresenceDTO dto = new TeamPresenceDTO();
            dto.setEmployeeId(employee.getId());
            dto.setEmployeeName(employee.getFullName());

            OfficeTimings timings =
                    getEffectiveTimingsForEmployee(employee);

            dto.setEffectiveStartTime(timings.getStartTime());
            dto.setEffectiveEndTime(timings.getEndTime());

            LocalTime firstCheckIn =
                    attendanceEventRepository
                            .findFirstByEmployeeIdAndEventTimestampBetweenAndEventTypeOrderByEventTimestampAsc(
                                    employee.getId(),
                                    date.atStartOfDay(),
                                    date.atTime(LocalTime.MAX),
                                    AttendanceEvent.EventType.CONNECT
                            )
                            .map(ev ->
                                    ev.getEventTimestamp().toLocalTime())
                            .orElse(null);

            dto.setFirstCheckIn(firstCheckIn);

            if (firstCheckIn != null) {
                dto.setStatus(
                        firstCheckIn.isAfter(timings.getStartTime())
                                ? "Late"
                                : "Present"
                );
            } else {
                boolean onLeave =
                        leaveRepository
                                .existsByEmployeeIdAndDateAndStatusNot(
                                        employee.getId(),
                                        date,
                                        LeaveStatus.CANCELLED
                                );

                if (onLeave) {
                    dto.setStatus("On Leave");
                } else if (isNonWorkingDay(date)) {
                    dto.setStatus("Weekend/Holiday");
                } else {
                    dto.setStatus("Absent");
                }
            }

            TimelineResponseDTO timelineResponse =
                    getEmployeeTimelineForDate(employee.getId(), date);

            dto.setTimelinePeriods(
                    timelineResponse.getPeriods()
            );

            return dto;

        }).collect(Collectors.toList());
    }

    // ============================================================
    // 7️⃣ CHECK WEEKEND / HOLIDAY
    // ============================================================
    private boolean isNonWorkingDay(LocalDate date) {

        if (date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return true;
        }

        return holidayRepository
                .findByHolidayDate(date)
                .isPresent();
    }

    // ============================================================
    // 🔹 INTERNAL DTO CLASSES
    // ============================================================
    @Getter
    @AllArgsConstructor
    public static class OfficeTimings {
        private final LocalTime startTime;
        private final LocalTime endTime;
    }

    @Getter
    @AllArgsConstructor
    public static class OfficeLocation {
        private final double latitude;
        private final double longitude;
        private final double radius;
    }
}
