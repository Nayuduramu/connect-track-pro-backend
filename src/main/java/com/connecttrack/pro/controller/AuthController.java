package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.LoginRequest;
import com.connecttrack.pro.dto.LoginResponse;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.entity.EmployeeStatus; // ✅ IMPORTANT
import com.connecttrack.pro.repository.EmployeeRepository;
import com.connecttrack.pro.security.JwtUtil;
import com.connecttrack.pro.service.EmailService;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    // =========================
    // LOGIN (PLAIN TEXT)
    // =========================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        // ✅ VALIDATION
        if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email and password are required"));
        }

        Employee employee = employeeRepository.findByEmail(loginRequest.getEmail())
                .orElse(null);

        if (employee == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not found"));
        }

        // ✅ STATUS CHECK (ENUM FIX)
        if (employee.getStatus() != null && employee.getStatus() != EmployeeStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "User is not active"));
        }

        // ✅ PASSWORD CHECK (PLAIN TEXT)
        if (!loginRequest.getPassword().equals(employee.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        // ✅ DEVICE CHECK
        if (employee.getDeviceId() != null &&
                loginRequest.getDeviceId() != null &&
                !employee.getDeviceId().equals(loginRequest.getDeviceId())) {

            Map<String, Object> error = new HashMap<>();
            error.put("message", "Device mismatch");
            error.put("deviceMismatch", true);

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        // ✅ GENERATE TOKEN
        String jwt = jwtUtil.generateToken(employee);

        // ✅ RESPONSE
        LoginResponse response = new LoginResponse();
        response.setToken(jwt);
        response.setId(employee.getId());
        response.setFullName(employee.getFullName());
        response.setEmail(employee.getEmail());
        response.setRole(employee.getRole().getName());
        response.setPasswordChangeRequired(false);
        response.setDeviceMismatch(false);

        // Optional fields
        response.setProfilePictureUrl(employee.getProfilePictureUrl());

        if (employee.getDepartment() != null) {
            response.setDepartmentName(employee.getDepartment().getName());
        }

        response.setJoinDate(employee.getJoinDate());

        return ResponseEntity.ok(response);
    }

    // =========================
    // DEBUG RESET (TESTING ONLY)
    // =========================
    @PostMapping("/debug-reset")
    public ResponseEntity<?> debugReset() {

        Employee employee = employeeRepository
                .findByEmail("admin@company.com")
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        employee.setPassword("password"); // 🔥 plain text
        employee.setDeviceId("admin-device-001");
        employee.setPasswordChangeRequired(false);
        employee.setStatus(EmployeeStatus.ACTIVE); // ✅ ensure active

        employeeRepository.save(employee);

        return ResponseEntity.ok(Map.of(
                "message", "Reset success",
                "email", employee.getEmail(),
                "password", "password"
        ));
    }

    // =========================
    // FORGOT PASSWORD
    // =========================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {

        String email = payload.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required"));
        }

        Employee employee = employeeRepository.findByEmail(email).orElse(null);

        if (employee != null) {
            String tempPassword = RandomStringUtils.randomAlphanumeric(8);

            employee.setPassword(tempPassword); // 🔥 plain (testing mode)
            employee.setPasswordChangeRequired(true);

            employeeRepository.save(employee);

            emailService.sendPasswordResetEmail(email, tempPassword);
        }

        return ResponseEntity.ok(Map.of(
                "message", "If account exists, reset email sent"
        ));
    }
}