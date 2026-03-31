// src\main\java\com\connecttrack\pro\entity\Employee.java
package com.connecttrack.pro.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "employees")

public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 60)
    private String password;

    // --- THIS IS THE CORRECTED MAPPING ---
    @ManyToOne(fetch = FetchType.EAGER) // Defines the relationship (Many Employees to One Role)
    @JoinColumn(name = "role_id", nullable = false) // Specifies the foreign key column in the 'employees' table
    private Role role;
    // ------------------------------------

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false)
    private boolean passwordChangeRequired = true;

    @Enumerated(EnumType.STRING) // This is CORRECT because EmployeeStatus IS an enum
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    private LocalDate joinDate;
    private String deviceId; // For device binding

    // For custom office timings per employee
    private LocalTime customStartTime;
    private LocalTime customEndTime;

    // This will store the publicly accessible URL of the profile photo.
    @Column(length = 512)
    private String profilePictureUrl;

    // Add this inside the Employee.java class
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;

    // --- NEW FIELDS FOR CUSTOM LOCATION ---
    private Double officeLatitude;
    private Double officeLongitude;
    private Double officeRadius;

}