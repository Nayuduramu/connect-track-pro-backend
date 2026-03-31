// File: src/main/java/com/connecttrack/pro/service/EmployeeService.java
package com.connecttrack.pro.service;

import com.connecttrack.pro.dto.ApproverDTO;
import com.connecttrack.pro.dto.ChangePasswordRequest;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.entity.Role;
import com.connecttrack.pro.repository.EmployeeRepository;
import com.connecttrack.pro.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private RoleRepository roleRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;


    // -----------------------------------------------------------------------
    // REGISTER DEVICE
    // -----------------------------------------------------------------------
    @Transactional
    public void registerDevice(String deviceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found, cannot register device."));

        employee.setDeviceId(deviceId);
        employeeRepository.save(employee);
    }


    // -----------------------------------------------------------------------
    // CHANGE PASSWORD
    // -----------------------------------------------------------------------
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));

        if (!passwordEncoder.matches(request.getOldPassword(), employee.getPassword())) {
            throw new IllegalArgumentException("Incorrect old password.");
        }

        employee.setPassword(passwordEncoder.encode(request.getNewPassword()));
        employeeRepository.save(employee);
    }


    // -----------------------------------------------------------------------
    // UPDATE PROFILE PICTURE (FIXED VERSION)
    // -----------------------------------------------------------------------
    @Transactional
    public String updateProfilePicture(MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));

        // Store file and get new filename
        String filename = fileStorageService.store(file);

        // Store only relative path
        String relativePath = "/public/images/" + filename;

        employee.setProfilePictureUrl(relativePath);
        employeeRepository.save(employee);

        return relativePath;
    }


    // -----------------------------------------------------------------------
    // REMOVE PROFILE PICTURE  **NEW METHOD**
    // -----------------------------------------------------------------------
    @Transactional
    public void removeProfilePicture() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));

        // Remove only DB reference (optional: delete actual file)
        employee.setProfilePictureUrl(null);
        employeeRepository.save(employee);
    }


    // -----------------------------------------------------------------------
    // FIND APPROVERS
    // -----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ApproverDTO> findApproversForEmployee(Long employeeId) {
        List<ApproverDTO> approvers = new ArrayList<>();

        Employee currentUser = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        // Add Section Admin
        if (currentUser.getManager() != null) {
            Employee sectionAdmin = currentUser.getManager();
            approvers.add(new ApproverDTO(sectionAdmin.getId(), sectionAdmin.getFullName(), "Section Admin"));
        }

        // Add Super Admins
        Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROLE_SUPER_ADMIN role not found in database"));

        List<Employee> superAdmins = employeeRepository.findByRole(superAdminRole);

        for (Employee superAdmin : superAdmins) {
            boolean alreadyAdded = approvers.stream()
                    .anyMatch(a -> a.getId().equals(superAdmin.getId()));
            if (!alreadyAdded) {
                approvers.add(new ApproverDTO(superAdmin.getId(), superAdmin.getFullName(), "Super Admin"));
            }
        }

        if (approvers.isEmpty()) {
            throw new RuntimeException("No approvers found for this employee. Please configure approvers.");
        }

        return approvers;
    }
}
