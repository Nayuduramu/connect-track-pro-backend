package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.GrantLeaveRequest;
import com.connecttrack.pro.dto.LeaveDTO;
import com.connecttrack.pro.dto.LeaveRequestDTO;
import com.connecttrack.pro.dto.LeaveStatusUpdateRequestDTO;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.entity.Leave;
import com.connecttrack.pro.mapper.LeaveMapper;
import com.connecttrack.pro.repository.EmployeeRepository;
import com.connecttrack.pro.security.CustomUserDetails;
import com.connecttrack.pro.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveMapper leaveMapper;
    private final EmployeeRepository employeeRepository; // This should ideally be removed from controller

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'SECTION_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> requestLeave(@RequestBody LeaveRequestDTO leaveRequest, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Leave createdLeave = leaveService.createLeaveRequest(leaveRequest, userDetails.getId());
        return ResponseEntity.ok(leaveMapper.toDto(createdLeave));
    }
    
    @PostMapping("/grant")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_SECTION_ADMIN')")
    public ResponseEntity<?> grantLeave(@RequestBody GrantLeaveRequest request) {
        // This logic SHOULD BE in LeaveService. We will add the method there.
        try {
            Leave leave = leaveService.grantLeaveToEmployee(request); // Assuming this method will be created
            return ResponseEntity.ok(leaveMapper.toDto(leave));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
    
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveDTO>> getMyLeaves(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Leave> leaves = leaveService.getLeavesForEmployee(userDetails.getId());
        return ResponseEntity.ok(leaves.stream().map(leaveMapper::toDto).collect(Collectors.toList()));
    }
    
    @GetMapping("/admin/view")
    @PreAuthorize("hasAnyRole('SECTION_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<LeaveDTO>> getLeavesForAdminView(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Leave> leaves = leaveService.getLeavesForAdmin(userDetails.getId());
        return ResponseEntity.ok(leaves.stream().map(leaveMapper::toDto).collect(Collectors.toList()));
    }
    
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SECTION_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> approveLeave(@PathVariable Long id, @RequestBody(required = false) LeaveStatusUpdateRequestDTO request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Leave updatedLeave = leaveService.approveLeave(id, userDetails.getId(), request != null ? request.getComment() : null);
        return ResponseEntity.ok(leaveMapper.toDto(updatedLeave));
    }
    
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SECTION_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> rejectLeave(@PathVariable Long id, @RequestBody(required = false) LeaveStatusUpdateRequestDTO request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Leave updatedLeave = leaveService.rejectLeave(id, userDetails.getId(), request != null ? request.getComment() : null);
        return ResponseEntity.ok(leaveMapper.toDto(updatedLeave));
    }
    
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SECTION_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> cancelLeave(@PathVariable Long id, @RequestBody(required = false) LeaveStatusUpdateRequestDTO request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Leave updatedLeave = leaveService.cancelLeaveByAdmin(id, userDetails.getId(), request != null ? request.getComment() : null);
        return ResponseEntity.ok(leaveMapper.toDto(updatedLeave));
    }

    // This endpoint is redundant if getLeavesForAdmin works correctly
    // It can be removed, but for now we'll assume it calls a new service method
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<LeaveDTO>> getAllLeaves() {
        List<Leave> leaves = leaveService.getAllLeaves(); // Assuming this method will be created
        return ResponseEntity.ok(leaves.stream().map(leaveMapper::toDto).collect(Collectors.toList()));
    }
}