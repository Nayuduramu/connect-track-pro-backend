// File: src/main/java/com/connecttrack/pro/controller/EmployeeController.java
package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.ApproverDTO;
import com.connecttrack.pro.dto.ChangePasswordRequest;
import com.connecttrack.pro.security.CustomUserDetails;
import com.connecttrack.pro.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * Registers a device for the logged-in user.
     */
    @PostMapping("/register-device")
    public ResponseEntity<?> registerDevice(@RequestBody Map<String, String> payload) {
        String deviceId = payload.get("deviceId");
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Device ID cannot be empty.");
        }
        employeeService.registerDevice(deviceId);
        return ResponseEntity.ok().build();
    }

    /**
     * Allows a user to change their password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            employeeService.changePassword(changePasswordRequest);
            return ResponseEntity.ok("Password changed successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred while changing the password.");
        }
    }

    /**
     * Uploads a profile photo and returns its public URL.
     */
    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadProfilePhoto(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = employeeService.updateProfilePicture(file);
            return ResponseEntity.ok(Collections.singletonMap("url", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Could not upload the file: " + e.getMessage());
        }
    }

    /**
     * Deletes (removes) the user's profile photo.
     */
    @DeleteMapping("/profile-photo")
    public ResponseEntity<?> removeProfilePhoto() {
        try {
            employeeService.removeProfilePicture();
            return ResponseEntity.ok("Profile picture removed successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Could not remove profile picture: " + e.getMessage());
        }
    }

    /**
     * Fetches available approvers for the logged-in employee.
     */
    @GetMapping("/approvers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApproverDTO>> getApprovers(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ApproverDTO> approvers = employeeService.findApproversForEmployee(userDetails.getId());
        return ResponseEntity.ok(approvers);
    }
}
