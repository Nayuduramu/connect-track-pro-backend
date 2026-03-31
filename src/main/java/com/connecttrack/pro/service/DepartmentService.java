package com.connecttrack.pro.service;

import com.connecttrack.pro.entity.Department;
import com.connecttrack.pro.repository.DepartmentRepository;
import com.connecttrack.pro.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department createDepartment(Department department) {
        // Optional: Add validation for name, etc.
        return departmentRepository.save(department);
    }

    public Department updateDepartment(Long id, Department departmentDetails) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        department.setName(departmentDetails.getName());
        return departmentRepository.save(department);
    }

    public void deleteDepartment(Long id) {
        // --- BUSINESS RULE ENFORCEMENT ---
        // Check if any employees are assigned to this department before deleting.
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new IllegalStateException("Cannot delete department: It still has employees assigned to it.");
        }
        departmentRepository.deleteById(id);
    }
}