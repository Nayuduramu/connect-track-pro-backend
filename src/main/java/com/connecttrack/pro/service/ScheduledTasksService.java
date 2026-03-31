package com.connecttrack.pro.service;

import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.entity.Leave;
import com.connecttrack.pro.entity.LeaveStatus;
import com.connecttrack.pro.entity.LeaveStatusHistory;
import com.connecttrack.pro.repository.EmployeeRepository;
import com.connecttrack.pro.repository.LeaveRepository;
import com.connecttrack.pro.repository.LeaveStatusHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledTasksService {

    private final LeaveRepository leaveRepository;
    private final LeaveStatusHistoryRepository historyRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * -----------------------------------------------------------------------
     *  CRON JOB TO UPDATE LEAVE STATUS
     *  Runs automatically EVERY DAY at 1:00 AM server time.
     * -----------------------------------------------------------------------
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void updateCompletedLeavesToUtilized() {

        log.info("=== [SCHEDULER] Starting check for completed leaves... ===");

        // 1️⃣ Find all APPROVED leaves whose end date is BEFORE today
        List<Leave> completedLeaves = leaveRepository
                .findAllByStatusAndToDateBefore(LeaveStatus.APPROVED, LocalDate.now());

        if (completedLeaves.isEmpty()) {
            log.info("[SCHEDULER] No approved leaves to update.");
            return;
        }

        // 2️⃣ Find a ‘System’ or ‘Super Admin’ user to attribute the automated update
        Employee systemUser = employeeRepository
                .findByRoleName("ROLE_SUPER_ADMIN")
                .stream()
                .findFirst()
                .orElse(null);

        if (systemUser == null) {
            log.error("[SCHEDULER] ❌ No Super Admin found! Cannot update leaves.");
            return;
        }

        // 3️⃣ Update each leave & insert a history change record
        for (Leave leave : completedLeaves) {

            log.info("[SCHEDULER] Updating Leave ID {} → UTILIZED", leave.getId());

            LeaveStatus oldStatus = leave.getStatus();
            leave.setStatus(LeaveStatus.UTILIZED);

            // Save history entry
            LeaveStatusHistory history = new LeaveStatusHistory();
            history.setLeave(leave);
            history.setOldStatus(oldStatus);
            history.setNewStatus(LeaveStatus.UTILIZED);
            history.setChangedBy(systemUser);
            history.setComment("Status automatically updated after leave completion.");
            history.setChangedAt(LocalDateTime.now());

            historyRepository.save(history);
        }

        // 4️⃣ Save all updated leaves at once
        leaveRepository.saveAll(completedLeaves);

        log.info("[SCHEDULER] ✅ Updated {} leave(s) to UTILIZED.", completedLeaves.size());
    }
}
