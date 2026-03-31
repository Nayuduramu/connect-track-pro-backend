package com.connecttrack.pro.repository;

import com.connecttrack.pro.entity.AttendanceEvent;
import com.connecttrack.pro.entity.Employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, Long> {

    // -------------------------------------------------------------------------
    // 1️⃣ Fetch all events for employee within a date-time range
    // -------------------------------------------------------------------------
    List<AttendanceEvent> findByEmployeeIdAndEventTimestampBetweenOrderByEventTimestampAsc(
            Long employeeId,
            LocalDateTime startTimestamp,
            LocalDateTime endTimestamp
    );

    // -------------------------------------------------------------------------
    // 2️⃣ First event of a type on a day
    // -------------------------------------------------------------------------
    Optional<AttendanceEvent> findFirstByEmployeeIdAndEventTimestampBetweenAndEventTypeOrderByEventTimestampAsc(
            Long employeeId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            AttendanceEvent.EventType eventType
    );

    // -------------------------------------------------------------------------
    // 3️⃣ Delete all events for an employee
    // -------------------------------------------------------------------------
    void deleteByEmployeeId(Long employeeId);

    // -------------------------------------------------------------------------
    // 4️⃣ Count late employees (Super Admin)
    // -------------------------------------------------------------------------
    @Query(value =
            "SELECT COUNT(DISTINCT employee_id) FROM attendance_events " +
            "WHERE DATE(event_timestamp) = :date " +
            "AND event_type = 'CONNECT' " +
            "AND TIME(event_timestamp) > (SELECT setting_value FROM app_settings WHERE setting_key = 'office_start_time')",
            nativeQuery = true)
    long countLateEmployeesToday(@Param("date") LocalDate date);

    // -------------------------------------------------------------------------
    // 5️⃣ Count late employees for selected IDs (Section Admin)
    // -------------------------------------------------------------------------
    @Query(value =
            "SELECT COUNT(DISTINCT employee_id) FROM attendance_events " +
            "WHERE DATE(event_timestamp) = :date " +
            "AND event_type = 'CONNECT' " +
            "AND employee_id IN :employeeIds " +
            "AND TIME(event_timestamp) > (SELECT setting_value FROM app_settings WHERE setting_key = 'office_start_time')",
            nativeQuery = true)
    long countLateEmployeesTodayForIds(
            @Param("date") LocalDate date,
            @Param("employeeIds") List<Long> employeeIds
    );

    // -------------------------------------------------------------------------
    // 6️⃣ Fetch unique employees who connected on given date
    // -------------------------------------------------------------------------
    @Query("SELECT DISTINCT a.employee.id FROM AttendanceEvent a " +
            "WHERE DATE(a.eventTimestamp) = :date AND a.eventType = 'CONNECT'")
    List<Long> findDistinctEmployeeIdsConnectedOnDate(@Param("date") LocalDate date);

    // -------------------------------------------------------------------------
    // 7️⃣ Corrected late count query for dashboard (section-based)
    // -------------------------------------------------------------------------
    @Query("SELECT COUNT(DISTINCT e.employee.id) FROM AttendanceEvent e " +
            "WHERE e.employee.id IN :employeeIds " +
            "AND DATE(e.eventTimestamp) = :date " +
            "AND e.eventType = 'CONNECT' " +
            "AND TIME(e.eventTimestamp) > (SELECT a.settingValue FROM AppSetting a WHERE a.settingKey = 'office_start_time')")
    long countLateEmployeesForIdsOnDate(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("date") LocalDate date
    );

    // -------------------------------------------------------------------------
    // 8️⃣ Employees who connected (dashboard)
    // -------------------------------------------------------------------------
    @Query("SELECT DISTINCT e.employee.id FROM AttendanceEvent e " +
            "WHERE DATE(e.eventTimestamp) = :date AND e.eventType = 'CONNECT'")
    List<Long> findDistinctEmployeeIdsConnectedOnDateDashboard(@Param("date") LocalDate date);

    // -------------------------------------------------------------------------
    // 9️⃣ Latest event for router (existing)
    // -------------------------------------------------------------------------
    Optional<AttendanceEvent> findTopByEmployeeAndRouterMacAndEventTypeOrderByEventTimestampDesc(
            Employee employee,
            String routerMac,
            AttendanceEvent.EventType eventType
    );

    // -------------------------------------------------------------------------
    // 🔟 NEW REQUIRED METHOD – find latest event BEFORE a given timestamp
    // -------------------------------------------------------------------------
    Optional<AttendanceEvent> findTopByEmployeeAndRouterMacAndEventTypeAndEventTimestampBeforeOrderByEventTimestampDesc(
            Employee employee,
            String routerMac,
            AttendanceEvent.EventType eventType,
            LocalDateTime timestamp
    );
}
