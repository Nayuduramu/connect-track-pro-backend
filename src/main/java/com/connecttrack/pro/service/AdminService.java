package com.connecttrack.pro.service;

import com.connecttrack.pro.dto.AdminDashboardStatsDTO;
import com.connecttrack.pro.dto.CreateEmployeeRequest;
import com.connecttrack.pro.dto.EditEmployeeRequest;
import com.connecttrack.pro.dto.EmployeeDTO;
import com.connecttrack.pro.entity.*;
import com.connecttrack.pro.mapper.EmployeeMapper;
import com.connecttrack.pro.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {

    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmployeeMapper employeeMapper;
    @Autowired private LeaveRepository leaveRepository;
    @Autowired private AttendanceEventRepository attendanceEventRepository;

    // ---------------------------------------------------------
    // 🔍 FETCH EMPLOYEES BASED ON ADMIN ROLE
    // ---------------------------------------------------------
    public List<EmployeeDTO> getAllEmployees() {
        Employee current = getCurrentUser();
        List<Employee> employees;

        if ("ROLE_SUPER_ADMIN".equals(current.getRole().getName())) {
            employees = employeeRepository.findAll();
        } else if ("ROLE_SECTION_ADMIN".equals(current.getRole().getName())
                && current.getDepartment() != null) {
            employees = employeeRepository.findAllByDepartmentOrderByIdAsc(current.getDepartment());
        } else {
            return Collections.emptyList();
        }

        return employees.stream()
                .map(employeeMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------
    // 👤 CREATE EMPLOYEE  (SUPER + SECTION ADMIN)
    // ---------------------------------------------------------
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SECTION_ADMIN')")
    public EmployeeDTO createEmployee(CreateEmployeeRequest request) {

        if (employeeRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        // ⭐ FIX: Support Select_All (ID = 0)
        Department department = null;
        if (request.getDepartmentId() != 0) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException(
                            "Department not found with ID: " + request.getDepartmentId()));
        }

        Role roleToAssign = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> new RuntimeException(
                        "Role not found: " + request.getRole()));

        Employee creator = getCurrentUser();

        // ⭐ Section Admin security
        if ("ROLE_SECTION_ADMIN".equals(creator.getRole().getName())) {

            // Can't create admins
            if (!"ROLE_EMPLOYEE".equals(roleToAssign.getName())) {
                throw new AccessDeniedException("Section Admins can only create EMPLOYEES.");
            }

            // Must assign employees only to own department
            if (department == null || !department.getId().equals(creator.getDepartment().getId())) {
                throw new AccessDeniedException("Section Admins can assign only to their own department.");
            }
        }

        Employee newEmp = new Employee();
        newEmp.setFullName(request.getFullName());
        newEmp.setEmail(request.getEmail());
        newEmp.setPassword(passwordEncoder.encode(request.getPassword()));
        newEmp.setRole(roleToAssign);
        newEmp.setDepartment(department);
        newEmp.setPasswordChangeRequired(true);
        newEmp.setStatus(EmployeeStatus.ACTIVE);
        newEmp.setJoinDate(LocalDate.now());

        return employeeMapper.toDTO(employeeRepository.save(newEmp));
    }

    // ---------------------------------------------------------
    // ✏️ EDIT EMPLOYEE
    // ---------------------------------------------------------
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SECTION_ADMIN')")
    public EmployeeDTO editEmployee(Long employeeId, EditEmployeeRequest request) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        // ⭐ FIX: Support Select_All (ID = 0)
        Department newDept = null;
        if (request.getDepartmentId() != 0) {
            newDept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + request.getDepartmentId()));
        }

        Role newRole = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));

        Employee editor = getCurrentUser();

        // ⭐ Section admin restrictions
        if ("ROLE_SECTION_ADMIN".equals(editor.getRole().getName())) {

            // Cannot promote anyone
            if (!"ROLE_EMPLOYEE".equals(newRole.getName())) {
                throw new AccessDeniedException("Section Admins cannot assign admin roles.");
            }

            // Can edit ONLY own department employees
            if (!Objects.equals(editor.getDepartment(), employee.getDepartment())) {
                throw new AccessDeniedException("You cannot edit employees from another department.");
            }

            // Cannot move employees to Select_All or another department
            if (newDept == null ||
                !newDept.getId().equals(editor.getDepartment().getId())) {
                throw new AccessDeniedException("Section Admins cannot change the department.");
            }
        }

        employee.setFullName(request.getFullName());
        employee.setEmail(request.getEmail());
        employee.setRole(newRole);
        employee.setDepartment(newDept);

        return employeeMapper.toDTO(employeeRepository.save(employee));
    }

    // ---------------------------------------------------------
    // 🚫 DEACTIVATE / ACTIVATE EMPLOYEE
    // ---------------------------------------------------------
    public void deactivateEmployee(Long employeeId) {
        Employee current = getCurrentUser();

        if (current.getId().equals(employeeId)) {
            throw new IllegalArgumentException("You cannot deactivate your own account.");
        }

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found."));

        emp.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(emp);
    }

    public void activateEmployee(Long employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found."));

        emp.setStatus(EmployeeStatus.ACTIVE);
        employeeRepository.save(emp);
    }

    // ---------------------------------------------------------
    // ⏰ CUSTOM TIMINGS
    // ---------------------------------------------------------
    public EmployeeDTO setCustomEmployeeTimings(Long id, Map<String, String> timings) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found."));

        LocalTime start = (timings.get("startTime") != null && !timings.get("startTime").isEmpty())
                ? LocalTime.parse(timings.get("startTime"))
                : null;

        LocalTime end = (timings.get("endTime") != null && !timings.get("endTime").isEmpty())
                ? LocalTime.parse(timings.get("endTime"))
                : null;

        emp.setCustomStartTime(start);
        emp.setCustomEndTime(end);

        return employeeMapper.toDTO(employeeRepository.save(emp));
    }

    // ---------------------------------------------------------
    // 📱 CLEAR DEVICE
    // ---------------------------------------------------------
    public void clearDevice(Long employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found."));
        emp.setDeviceId(null);
        employeeRepository.save(emp);
    }

    // ---------------------------------------------------------
    // 📊 DASHBOARD STATS
    // ---------------------------------------------------------
    public AdminDashboardStatsDTO getStaffStats() {
        Employee admin = getCurrentUser();
        AdminDashboardStatsDTO stats = new AdminDashboardStatsDTO();
        LocalDate today = LocalDate.now();

        List<Employee> employees;

        if ("ROLE_SUPER_ADMIN".equals(admin.getRole().getName())) {
            employees = employeeRepository.findAll();
        } else if ("ROLE_SECTION_ADMIN".equals(admin.getRole().getName())
                && admin.getDepartment() != null) {
            employees = employeeRepository.findAllByDepartmentOrderByIdAsc(admin.getDepartment());
        } else {
            return new AdminDashboardStatsDTO();
        }

        long inactive = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.INACTIVE).count();
        stats.setInactiveCount(inactive);

        List<Employee> active = employees.stream()
                .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE).toList();

        stats.setTotalStaff((long) active.size());

        if (active.isEmpty()) return stats;

        List<Long> ids = active.stream().map(Employee::getId).toList();

        long onLeave = leaveRepository.countByEmployeeIdInAndFromDateLessThanEqualAndToDateGreaterThanEqualAndStatusNot(
                ids, today, today, LeaveStatus.CANCELLED);
        stats.setTodayOnLeaveCount(onLeave);

        long late = attendanceEventRepository.countLateEmployeesForIdsOnDate(ids, today);
        stats.setTodayLateCount(late);

        List<Long> connectedToday =
                attendanceEventRepository.findDistinctEmployeeIdsConnectedOnDate(today);

        long connectedCount = connectedToday.stream().filter(ids::contains).count();
        stats.setConnectedCount(connectedCount);

        long notConnected = active.size() - connectedCount - onLeave;
        stats.setNotConnectedCount(Math.max(0, notConnected));

        return stats;
    }


     // --- NEW METHOD Custom Location ---
    public EmployeeDTO setCustomEmployeeLocation(Long employeeId, Map<String, String> locationData) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (locationData.containsKey("latitude") && locationData.containsKey("longitude")) {
            employee.setOfficeLatitude(Double.parseDouble(locationData.get("latitude")));
            employee.setOfficeLongitude(Double.parseDouble(locationData.get("longitude")));
            employee.setOfficeRadius(Double.parseDouble(locationData.get("radius")));
        } else {
            // If data is missing/empty, reset to null (use default)
            employee.setOfficeLatitude(null);
            employee.setOfficeLongitude(null);
            employee.setOfficeRadius(null);
        }

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toDTO(saved);
    }

    // ---------------------------------------------------------
    // 🔐 CURRENT LOGGED-IN USER
    // ---------------------------------------------------------
    private Employee getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found."));
    }
}
