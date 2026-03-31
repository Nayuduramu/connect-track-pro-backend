// src/main/java/com/connecttrack/pro/repository/EmployeeRepository.java
package com.connecttrack.pro.repository;

import com.connecttrack.pro.entity.Department;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.entity.EmployeeStatus;
import com.connecttrack.pro.entity.Role;      // <-- REQUIRED IMPORT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;      // <-- REQUIRED IMPORT
import org.springframework.data.repository.query.Param;   // <-- REQUIRED IMPORT
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // ---------------------------------------------------------------
    // 🔐 AUTHENTICATION
    // ---------------------------------------------------------------
    Optional<Employee> findByEmail(String email);

    // ---------------------------------------------------------------
    // 🔍 LOOKUP BY FULL NAME (used for WebSocket chat mapping)
    // ---------------------------------------------------------------
    Optional<Employee> findByFullName(String fullName);

    // ---------------------------------------------------------------
    // 📌 Department-based List
    // ---------------------------------------------------------------
    List<Employee> findAllByDepartmentOrderByIdAsc(Department department);

    // ---------------------------------------------------------------
    // 📊 COUNT employees by status (used in dashboards)
    // ---------------------------------------------------------------
    long countByStatus(EmployeeStatus status);

    long countByStatusAndDepartment(EmployeeStatus status, Department department);

    // ---------------------------------------------------------------
    // 📋 Basic queries
    // ---------------------------------------------------------------
    List<Employee> findAllByStatus(EmployeeStatus status);

    List<Employee> findAllByDepartmentAndStatus(Department department, EmployeeStatus status);

    // ---------------------------------------------------------------
    // 🔑 Find by Role entity directly
    // ---------------------------------------------------------------
    List<Employee> findByRole(Role role);

    // ---------------------------------------------------------------
    // 🆕 REQUIRED FOR SCHEDULER — find by Role Name (e.g. ROLE_SUPER_ADMIN)
    // ---------------------------------------------------------------
    @Query("SELECT e FROM Employee e WHERE e.role.name = :roleName")
    List<Employee> findByRoleName(@Param("roleName") String roleName);

    // ---------------------------------------------------------------
    // 🆕 Check if employees exist inside a department (for safe deletion)
    // ---------------------------------------------------------------
    boolean existsByDepartmentId(Long departmentId);
}
