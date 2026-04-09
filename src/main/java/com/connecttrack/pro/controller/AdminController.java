// src\main\java\com\connecttrack\pro\controller\AdminController.java
package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.*;
import com.connecttrack.pro.entity.*;
import com.connecttrack.pro.repository.*;
import com.connecttrack.pro.service.AdminService;
import com.connecttrack.pro.service.HolidayService;
import com.connecttrack.pro.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired private AdminService adminService;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AppSettingRepository appSettingRepository;
    @Autowired private WiFiRouterRepository wiFiRouterRepository;
    @Autowired private AttendanceEventRepository attendanceEventRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HolidayService holidayService;
    @Autowired private DepartmentService departmentService; // <-- NEW SERVICE

    // ================================
    // 🧑‍💼 EMPLOYEE MANAGEMENT
    // ================================

    @PostMapping("/employees")
    public ResponseEntity<?> createEmployee(@RequestBody CreateEmployeeRequest request) {
        try {
            EmployeeDTO newEmployee = adminService.createEmployee(request);
            return ResponseEntity.ok(newEmployee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<?> editEmployee(@PathVariable Long id, @RequestBody EditEmployeeRequest request) {
        try {
            EmployeeDTO updatedEmployee = adminService.editEmployee(id, request);
            return ResponseEntity.ok(updatedEmployee);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        return ResponseEntity.ok(adminService.getAllEmployees());
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<?> deactivateEmployee(@PathVariable Long id) {
        try {
            adminService.deactivateEmployee(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/employees/{id}/activate")
    public ResponseEntity<?> activateEmployee(@PathVariable Long id) {
        try {
            adminService.activateEmployee(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/employees/{id}/permanent")
    @Transactional
    public ResponseEntity<Void> deleteEmployeePermanently(@PathVariable Long id) {
        attendanceEventRepository.deleteByEmployeeId(id);
        employeeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/employees/{id}/reset-password")
    public ResponseEntity<?> resetEmployeePassword(@PathVariable Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        employee.setPassword(passwordEncoder.encode("welcome123"));
        employee.setPasswordChangeRequired(true);
        employeeRepository.save(employee);
        return ResponseEntity.ok("Password reset successfully. Temporary password: 'welcome123'.");
    }

    @PostMapping("/employees/{id}/timings")
    public ResponseEntity<EmployeeDTO> setCustomEmployeeTimings(
            @PathVariable Long id,
            @RequestBody Map<String, String> timings
    ) {
        return ResponseEntity.ok(adminService.setCustomEmployeeTimings(id, timings));
    }

    @PostMapping("/employees/{id}/clear-device")
    public ResponseEntity<?> clearDevice(@PathVariable Long id) {
        adminService.clearDevice(id);
        return ResponseEntity.ok().build();
    }

    // ================================
    // 📊 DASHBOARD STATS
    // ================================

    @GetMapping("/staff-stats")
    public ResponseEntity<AdminDashboardStatsDTO> getStaffStats() {
        return ResponseEntity.ok(adminService.getStaffStats());
    }

    // ================================
    // 🏢 DEPARTMENT MANAGEMENT (REFACTORED & ENHANCED)
    // ================================

    @PostMapping("/departments")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.createDepartment(department));
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SECTION_ADMIN')")
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Department> updateDepartment(
            @PathVariable Long id,
            @RequestBody Department departmentDetails
    ) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, departmentDetails));
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        try {
            departmentService.deleteDepartment(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // ================================
    // 📶 WIFI ROUTER MANAGEMENT
    // ================================

    @PostMapping("/wifi-routers")
    public ResponseEntity<WiFiRouter> addRouter(@RequestBody WiFiRouter router) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wiFiRouterRepository.save(router));
    }

    @GetMapping("/wifi-routers")
    public ResponseEntity<List<WiFiRouter>> getAllRouters() {
        return ResponseEntity.ok(wiFiRouterRepository.findAll());
    }

    @DeleteMapping("/wifi-routers/{id}")
    public ResponseEntity<Void> deleteRouter(@PathVariable Long id) {
        wiFiRouterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ================================
    // 📅 HOLIDAYS & WEEKENDS
    // ================================

    @PostMapping("/holidays")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Holiday> addHoliday(@RequestBody Holiday holiday) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.addHoliday(holiday));
    }

    @GetMapping("/holidays")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Holiday>> getAllHolidays() {
        return ResponseEntity.ok(holidayService.getAllHolidays());
    }

    @DeleteMapping("/holidays/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/holidays/toggle-weekend")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> toggleWeekend(@RequestBody Map<String, String> payload) {
        LocalDate date = LocalDate.parse(payload.get("date"));
        boolean isWorking = Boolean.parseBoolean(payload.get("isWorking"));
        holidayService.toggleWeekendWorkingStatus(date, isWorking);
        return ResponseEntity.ok().build();
    }

    // ================================
    // 🕒 OFFICE TIMINGS
    // ================================

    @GetMapping("/settings/timings")
    public ResponseEntity<Map<String, String>> getOfficeTimings() {
        AppSetting startTime = appSettingRepository.findById("office_start_time")
                .orElse(new AppSetting("office_start_time", "10:00"));
        AppSetting endTime = appSettingRepository.findById("office_end_time")
                .orElse(new AppSetting("office_end_time", "19:00"));
        Map<String, String> timings = new HashMap<>();
        timings.put("startTime", startTime.getSettingValue());
        timings.put("endTime", endTime.getSettingValue());
        return ResponseEntity.ok(timings);
    }

    @PostMapping("/settings/timings")
    public ResponseEntity<Void> setOfficeTimings(@RequestBody Map<String, String> timings) {
        appSettingRepository.save(new AppSetting("office_start_time", timings.get("startTime")));
        appSettingRepository.save(new AppSetting("office_end_time", timings.get("endTime")));
        return ResponseEntity.ok().build();
    }

    // ================================
    // 📍 OFFICE LOCATION
    // ================================

    @GetMapping("/settings/location")
    public ResponseEntity<Map<String, String>> getOfficeLocation() {
        AppSetting lat = appSettingRepository.findById("office_latitude")
                .orElse(new AppSetting("office_latitude", "0.0"));
        AppSetting lon = appSettingRepository.findById("office_longitude")
                .orElse(new AppSetting("office_longitude", "0.0"));
        AppSetting rad = appSettingRepository.findById("office_radius")
                .orElse(new AppSetting("office_radius", "100"));
        Map<String, String> location = new HashMap<>();
        location.put("latitude", lat.getSettingValue());
        location.put("longitude", lon.getSettingValue());
        location.put("radius", rad.getSettingValue());
        return ResponseEntity.ok(location);
    }

    @PostMapping("/settings/location")
    public ResponseEntity<Void> setOfficeLocation(@RequestBody Map<String, String> location) {
        appSettingRepository.save(new AppSetting("office_latitude", location.get("latitude")));
        appSettingRepository.save(new AppSetting("office_longitude", location.get("longitude")));
        appSettingRepository.save(new AppSetting("office_radius", location.get("radius")));
        return ResponseEntity.ok().build();
    }

    // --- NEW ENDPOINT for Custom Location ---
    @PostMapping("/employees/{id}/location")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_SECTION_ADMIN')")
    public ResponseEntity<EmployeeDTO> setCustomEmployeeLocation(
            @PathVariable Long id,
            @RequestBody Map<String, String> locationData
    ) {
        return ResponseEntity.ok(adminService.setCustomEmployeeLocation(id, locationData));
    }

    // ================================
    // 📅 WEEKEND SETTINGS
    // ================================

    @GetMapping("/settings/weekends")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Boolean>> getWeekendSettings() {
        AppSetting saturday = appSettingRepository.findById("WEEKEND_SATURDAY_IS_HOLIDAY")
                .orElse(new AppSetting("WEEKEND_SATURDAY_IS_HOLIDAY", "false"));
        AppSetting sunday = appSettingRepository.findById("WEEKEND_SUNDAY_IS_HOLIDAY")
                .orElse(new AppSetting("WEEKEND_SUNDAY_IS_HOLIDAY", "true"));
        Map<String, Boolean> settings = new HashMap<>();
        settings.put("saturday", Boolean.parseBoolean(saturday.getSettingValue()));
        settings.put("sunday", Boolean.parseBoolean(sunday.getSettingValue()));
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/settings/weekends")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> setWeekendSettings(@RequestBody Map<String, Boolean> settings) {
        appSettingRepository.save(new AppSetting("WEEKEND_SATURDAY_IS_HOLIDAY", settings.get("saturday").toString()));
        appSettingRepository.save(new AppSetting("WEEKEND_SUNDAY_IS_HOLIDAY", settings.get("sunday").toString()));
        return ResponseEntity.ok().build();
    }

    // ================================
    // ⚙️ GENERAL SETTINGS
    // ================================

    @PostMapping("/settings")
    public ResponseEntity<AppSetting> saveSetting(@RequestBody AppSetting setting) {
        return ResponseEntity.ok(appSettingRepository.save(setting));
    }
}
