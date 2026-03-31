package com.connecttrack.pro.mapper;

import com.connecttrack.pro.dto.LeaveDTO;
import com.connecttrack.pro.entity.Leave;
import org.springframework.stereotype.Component;

@Component
public class LeaveMapper {
    public LeaveDTO toDto(Leave leave) {
        if (leave == null) return null;

        LeaveDTO dto = new LeaveDTO();
        dto.setId(leave.getId());
        
        if (leave.getEmployee() != null) {
            dto.setEmployeeName(leave.getEmployee().getFullName());
        }
        
        if (leave.getLeaveType() != null) {
            // Use the enum's .name() method, which returns "CL" or "PERMISSION"
            dto.setLeaveTypeName(leave.getLeaveType().name()); 
        }

        dto.setFromDate(leave.getFromDate());
        dto.setToDate(leave.getToDate());
        dto.setReason(leave.getReason());

        if (leave.getStatus() != null) {
            dto.setStatus(leave.getStatus().name());
        }
        
        return dto;
    }
}