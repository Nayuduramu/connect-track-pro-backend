package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.LoginRequest;
import com.connecttrack.pro.dto.LoginResponse;
import com.connecttrack.pro.dto.SetPasswordRequest;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.repository.EmployeeRepository;
import com.connecttrack.pro.security.JwtUtil;
import com.connecttrack.pro.service.EmailService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // =========================
    // LOGIN
    // =========================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        Employee employee = employeeRepository.findByEmail(loginRequest.getEmail())
                .orElse(null);

        if (employee == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not found"));
        }

        boolean passwordMatched = passwordEncoder.matches(
                loginRequest.getPassword(),
                employee.getPassword()
        );

        if (!passwordMatched) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        // ✅ DEVICE CHECK
        if (employee.getDeviceId() != null &&
                loginRequest.getDeviceId() != null &&
                !employee.getDeviceId().equals(loginRequest.getDeviceId())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Device mismatch"));
        }

        // ✅ BUILD USER
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(employee.getEmail())
                .password(employee.getPassword())
                .authorities(employee.getRole().getName())
                .build();

        // ✅ PASSWORD CHANGE
        if (employee.isPasswordChangeRequired()) {
            String tempToken = jwtUtil.generatePasswordChangeToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("passwordChangeRequired", true);
            response.put("token", tempToken);

            return ResponseEntity.ok(response);
        }

        // ✅ JWT TOKEN
        String jwt = jwtUtil.generateToken(userDetails, employee);

        LoginResponse response = new LoginResponse();
        response.setToken(jwt);
        response.setId(employee.getId());
        response.setFullName(employee.getFullName());
        response.setEmail(employee.getEmail());
        response.setRole(employee.getRole().getName());
        response.setPasswordChangeRequired(false);
        response.setProfilePictureUrl(employee.getProfilePictureUrl());
        response.setJoinDate(employee.getJoinDate());

        if (employee.getDepartment() != null) {
            response.setDepartmentName(employee.getDepartment().getName());
        }

        return ResponseEntity.ok(response);
    }

    // =========================
    // DEBUG RESET
    // =========================
    @PostMapping("/debug-reset")
    public ResponseEntity<?> debugReset() {

        Employee employee = employeeRepository
                .findByEmail("admin@company.com")
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        employee.setPassword(passwordEncoder.encode("password"));
        employee.setPasswordChangeRequired(false);
        employee.setDeviceId("admin-device-001");

        employeeRepository.save(employee);

        return ResponseEntity.ok(Map.of("message", "Admin reset success"));
    }

    // =========================
    // SET PASSWORD
    // =========================
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody SetPasswordRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        employee.setPassword(passwordEncoder.encode(request.getNewPassword()));
        employee.setPasswordChangeRequired(false);

        employeeRepository.save(employee);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails, employee);

        return ResponseEntity.ok(Map.of("token", jwt));
    }

    // =========================
    // FORGOT PASSWORD
    // =========================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {

        String email = payload.get("email");

        Employee employee = employeeRepository.findByEmail(email).orElse(null);

        if (employee != null) {
            String tempPassword = RandomStringUtils.randomAlphanumeric(8);

            employee.setPassword(passwordEncoder.encode(tempPassword));
            employee.setPasswordChangeRequired(true);

            employeeRepository.save(employee);

            emailService.sendPasswordResetEmail(employee.getEmail(), tempPassword);
        }

        return ResponseEntity.ok(
                Map.of("message", "If account exists, reset email sent")
        );
    }
}