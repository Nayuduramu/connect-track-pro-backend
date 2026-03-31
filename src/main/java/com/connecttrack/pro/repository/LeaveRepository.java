package com.connecttrack.pro.repository;

import com.connecttrack.pro.entity.Leave;
import com.connecttrack.pro.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    // ----------------------------------------------------------------------
    // 1️⃣ Fetch all leaves approved by a specific approver (manager/admin)
    // ----------------------------------------------------------------------
    List<Leave> findByApproverId(Long approverId);

    // ----------------------------------------------------------------------
    // 2️⃣ Fetch all leaves of a specific employee (sorted by latest applied)
    // ----------------------------------------------------------------------
    List<Leave> findByEmployeeIdOrderByFromDateDesc(Long employeeId);

    // ----------------------------------------------------------------------
    // 3️⃣ Fetch all APPROVED leaves that ended before today (Scheduler uses this)
    // ----------------------------------------------------------------------
    @Query("SELECT l FROM Leave l WHERE l.status = :status AND l.toDate < :currentDate")
    List<Leave> findAllLeavesByStatusAndPastEndDate(
            @Param("status") LeaveStatus status,
            @Param("currentDate") LocalDate currentDate
    );

    // ----------------------------------------------------------------------
    // 4️⃣ Check if employee is on leave on a given date
    //     (excluding specific status — usually CANCELLED or REJECTED)
    // ----------------------------------------------------------------------
    @Query("""
           SELECT CASE WHEN COUNT(l) > 0 THEN TRUE ELSE FALSE END
           FROM Leave l
           WHERE l.employee.id = :employeeId
           AND :date BETWEEN l.fromDate AND l.toDate
           AND l.status <> :status
           """)
    boolean existsByEmployeeIdAndDateAndStatusNot(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date,
            @Param("status") LeaveStatus status
    );

    // ----------------------------------------------------------------------
    // 5️⃣ Count employees on leave in a date range
    //     (excluding cancelled/rejected)
    // ----------------------------------------------------------------------
    long countByEmployeeIdInAndFromDateLessThanEqualAndToDateGreaterThanEqualAndStatusNot(
            List<Long> employeeIds,
            LocalDate fromDate,
            LocalDate toDate,
            LeaveStatus status
    );

    // ----------------------------------------------------------------------
    // 6️⃣ ⭐ NEW REQUIRED METHOD ⭐
    //     Used by SCHEDULER to update completed leaves → UTILIZED
    // ----------------------------------------------------------------------
    List<Leave> findAllByStatusAndToDateBefore(LeaveStatus status, LocalDate date);
}
