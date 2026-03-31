package com.connecttrack.pro.service;

import com.connecttrack.pro.dto.GrantLeaveRequest;
import com.connecttrack.pro.dto.LeaveRequestDTO;
import com.connecttrack.pro.entity.*;
import com.connecttrack.pro.repository.EmployeeRepository;
import com.connecttrack.pro.repository.LeaveRepository;
import com.connecttrack.pro.repository.LeaveStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveStatusHistoryRepository historyRepository;

    @Override
    @Transactional
    public Leave createLeaveRequest(LeaveRequestDTO requestDTO, Long requesterId) {
        Employee requester = employeeRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        Employee approver = employeeRepository.findById(requestDTO.getApproverId())
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        Leave leave = new Leave();
        leave.setEmployee(requester);
        leave.setApprover(approver);
        leave.setFromDate(requestDTO.getFromDate());
        leave.setToDate(requestDTO.getToDate());
        leave.setReason(requestDTO.getReason());
        leave.setStatus(LeaveStatus.PENDING);

        // ✅ FIX: Safely convert leaveType string to enum
        if (requestDTO.getLeaveType() != null && !requestDTO.getLeaveType().isEmpty()) {
            try {
                LeaveType leaveTypeEnum = LeaveType.valueOf(requestDTO.getLeaveType().toUpperCase());
                leave.setLeaveType(leaveTypeEnum);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid leaveType value: " + requestDTO.getLeaveType());
            }
        } else {
            throw new IllegalArgumentException("LeaveType cannot be null or empty.");
        }

        Leave savedLeave = leaveRepository.save(leave);
        createHistory(savedLeave, null, LeaveStatus.PENDING, requester, "Leave requested by employee.");

        return savedLeave;
    }

    @Override
    public List<Leave> getLeavesForAdmin(Long adminId) {
        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole().getName().equals("ROLE_SUPER_ADMIN")) {
            return leaveRepository.findAll();
        } else {
            return leaveRepository.findByApproverId(adminId);
        }
    }

    @Override
    public List<Leave> getLeavesForEmployee(Long employeeId) {
        // ✅ Sorted by most recent fromDate
        return leaveRepository.findByEmployeeIdOrderByFromDateDesc(employeeId);
    }

    @Override
    @Transactional
    public Leave approveLeave(Long leaveId, Long adminId, String comment) {
        return updateLeaveStatus(leaveId, adminId, LeaveStatus.APPROVED, comment, LeaveStatus.PENDING);
    }

    @Override
    @Transactional
    public Leave rejectLeave(Long leaveId, Long adminId, String comment) {
        return updateLeaveStatus(leaveId, adminId, LeaveStatus.REJECTED, comment, LeaveStatus.PENDING);
    }

    @Override
    @Transactional
    public Leave cancelLeaveByAdmin(Long leaveId, Long adminId, String comment) {
        Leave leaveToCancel = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        // Admin can cancel PENDING or APPROVED leaves
        if (leaveToCancel.getStatus() == LeaveStatus.PENDING || leaveToCancel.getStatus() == LeaveStatus.APPROVED) {
            return updateLeaveStatus(leaveId, adminId, LeaveStatus.CANCELLED, comment, leaveToCancel.getStatus());
        } else {
            throw new IllegalStateException("Only PENDING or APPROVED leaves can be cancelled.");
        }
    }

    private Leave updateLeaveStatus(Long leaveId, Long adminId, LeaveStatus newStatus, String comment, LeaveStatus requiredOldStatus) {
        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != requiredOldStatus) {
            throw new IllegalStateException("Leave is not in " + requiredOldStatus + " status.");
        }

        if (!leave.getApprover().getId().equals(adminId)
                && !admin.getRole().getName().equals("ROLE_SUPER_ADMIN")) {
            throw new SecurityException("You are not authorized to action this leave request.");
        }

        LeaveStatus oldStatus = leave.getStatus();
        leave.setStatus(newStatus);

        createHistory(leave, oldStatus, newStatus, admin, comment);

        return leaveRepository.save(leave);
    }

    private void createHistory(Leave leave, LeaveStatus oldStatus, LeaveStatus newStatus, Employee changedBy, String comment) {
        LeaveStatusHistory history = new LeaveStatusHistory();
        history.setLeave(leave);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setComment(comment);
        history.setChangedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    // --- NEW METHOD: Admin can grant leave directly to employee ---
    @Override
    @Transactional
    public Leave grantLeaveToEmployee(GrantLeaveRequest request) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee admin = employeeRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee to grant leave to not found"));

        Leave leave = new Leave();
        leave.setEmployee(employee);
        leave.setFromDate(request.getFromDate());
        leave.setToDate(request.getToDate());
        leave.setReason(request.getReason());
        leave.setStatus(LeaveStatus.APPROVED); // Manually granted leaves are auto-approved
        leave.setApprover(admin);

        // ✅ Handle leaveType for granted leaves as well
        if (request.getLeaveType() != null && !request.getLeaveType().isEmpty()) {
            try {
                LeaveType leaveTypeEnum = LeaveType.valueOf(request.getLeaveType().toUpperCase());
                leave.setLeaveType(leaveTypeEnum);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid leaveType value: " + request.getLeaveType());
            }
        } else {
            throw new IllegalArgumentException("LeaveType cannot be null or empty.");
        }

        Leave savedLeave = leaveRepository.save(leave);
        createHistory(savedLeave, null, LeaveStatus.APPROVED, admin, "Leave granted directly by admin.");

        return savedLeave;
    }

    @Override
    public List<Leave> getAllLeaves() {
        // Typically a Super Admin function. Authorization handled at controller level.
        return leaveRepository.findAll();
    }
}
