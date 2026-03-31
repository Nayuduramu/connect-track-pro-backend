package com.connecttrack.pro.repository;

import com.connecttrack.pro.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // JpaRepository provides all the necessary CRUD methods like findAll(), findById(), save(), etc.
    // We can add custom query methods here later if needed.
}