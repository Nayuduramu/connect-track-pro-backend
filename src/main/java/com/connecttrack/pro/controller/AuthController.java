// src\main\java\com\connecttrack\pro\controller\AuthController.java
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

 
    @PostMapping("/login")
public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginRequest loginRequest) {

    System.out.println("LOGIN EMAIL = " + loginRequest.getEmail());
    System.out.println("REQUEST PASSWORD = " + loginRequest.getPassword());
    System.out.println("REQUEST DEVICE = " + loginRequest.getDeviceId());

    Employee employee = employeeRepository.findByEmail(loginRequest.getEmail())
            .orElse(null);

    if (employee == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
    }

    System.out.println("DB HASH = " + employee.getPassword());

    // ✅ Direct BCrypt password verification
    boolean passwordMatched = passwordEncoder.matches(
            loginRequest.getPassword(),
            employee.getPassword()
    );

    System.out.println("PASSWORD MATCHED = " + passwordMatched);

    if (!passwordMatched) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    // ✅ Device check
    String registeredDeviceId = employee.getDeviceId();
    String requestDeviceId = loginRequest.getDeviceId();

    System.out.println("DB Device ID = " + registeredDeviceId);
    System.out.println("Request Device ID = " + requestDeviceId);

    if (registeredDeviceId != null &&
            requestDeviceId != null &&
            !registeredDeviceId.equals(requestDeviceId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Device mismatch");
    }

    // ✅ Build token using email directly
    UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername(employee.getEmail())
            .password(employee.getPassword())
            .authorities(employee.getRole().getName())
            .build();

    // Password change required
    if (employee.isPasswordChangeRequired()) {
        String tempToken = jwtUtil.generatePasswordChangeToken(userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("passwordChangeRequired", true);
        response.put("token", tempToken);
        response.put("id", employee.getId());
        response.put("fullName", employee.getFullName());
        response.put("email", employee.getEmail());
        response.put("role", employee.getRole().getName());

        return ResponseEntity.ok(response);
    }

    final String jwt = jwtUtil.generateToken(userDetails, employee);

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

    // ===========================
    // DEBUG RESET FOR ADMIN TEST
    // ===========================
    @PostMapping("/debug-reset")
    public ResponseEntity<String> debugReset() {
        Employee employee = employeeRepository
                .findByEmail("admin@company.com")
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        employee.setPassword(passwordEncoder.encode("password"));
        employee.setPasswordChangeRequired(false);
        employee.setDeviceId("admin-device-001");

        employeeRepository.save(employee);

        return ResponseEntity.ok("Admin password reset successfully");
    }

    @PostMapping("/set-password")
    public ResponseEntity<?> setNewPassword(@RequestBody SetPasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        employee.setPassword(passwordEncoder.encode(request.getNewPassword()));
        employee.setPasswordChangeRequired(false);

        employeeRepository.save(employee);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        final String jwt = jwtUtil.generateToken(userDetails, employee);

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
                "If an account with that email exists, a password reset email has been sent."
        );
    }
}