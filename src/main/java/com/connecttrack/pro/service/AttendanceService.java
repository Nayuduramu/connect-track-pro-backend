package com.connecttrack.pro.service;

import com.connecttrack.pro.dto.AttendanceEventRequest;
import com.connecttrack.pro.entity.AttendanceEvent;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.repository.AttendanceEventRepository;
import com.connecttrack.pro.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceEventRepository attendanceEventRepository;
    private final EmployeeRepository employeeRepository;

    // -----------------------------------------------------------
    // ✅ FINAL, CORRECTED METHOD: Save attendance event
    // -----------------------------------------------------------
    @Transactional
    public void logAttendanceEvent(AttendanceEventRequest request) {

        // 1️⃣ Identify employee using Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found for email: " + userEmail));

        // 2️⃣ Parse event type
        AttendanceEvent.EventType eventType =
                AttendanceEvent.EventType.valueOf(request.getEventType().toUpperCase());

        // 3️⃣ Parse incoming timestamp (supports all fractional second formats)
        DateTimeFormatter flexibleFormatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .optionalEnd()
                .toFormatter();

        LocalDateTime eventTimestamp = LocalDateTime.parse(request.getTimestamp(), flexibleFormatter);

        // 4️⃣ Create event object
        AttendanceEvent event =
                new AttendanceEvent(employee, request.getRouterMac(), eventType, eventTimestamp);

        // 5️⃣ For DISCONNECT → compute session duration using matching CONNECT
        if (eventType == AttendanceEvent.EventType.DISCONNECT) {

            Optional<AttendanceEvent> lastConnectEvent =
                    attendanceEventRepository
                            .findTopByEmployeeAndRouterMacAndEventTypeAndEventTimestampBeforeOrderByEventTimestampDesc(
                                    employee,
                                    request.getRouterMac(),
                                    AttendanceEvent.EventType.CONNECT,
                                    eventTimestamp
                            );

            if (lastConnectEvent.isPresent()) {
                long durationMinutes =
                        Duration.between(lastConnectEvent.get().getEventTimestamp(), eventTimestamp)
                                .toMinutes();

                event.setSessionDurationMinutes(durationMinutes);
            }
        }

        // 6️⃣ Save the record
        attendanceEventRepository.save(event);
    }
}
