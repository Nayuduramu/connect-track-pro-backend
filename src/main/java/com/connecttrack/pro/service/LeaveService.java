package com.connecttrack.pro.service;

import com.connecttrack.pro.dto.GrantLeaveRequest; // <-- NEW IMPORT
import com.connecttrack.pro.dto.LeaveRequestDTO;
import com.connecttrack.pro.entity.Leave;
import java.util.List;

public interface LeaveService {
    Leave createLeaveRequest(LeaveRequestDTO leaveRequest, Long requesterId);
    List<Leave> getLeavesForAdmin(Long adminId);
    List<Leave> getLeavesForEmployee(Long employeeId);
    Leave approveLeave(Long leaveId, Long adminId, String comment);
    Leave rejectLeave(Long leaveId, Long adminId, String comment);
    Leave cancelLeaveByAdmin(Long leaveId, Long adminId, String comment);

    // --- NEW METHODS ---
    Leave grantLeaveToEmployee(GrantLeaveRequest request);
    List<Leave> getAllLeaves();
}