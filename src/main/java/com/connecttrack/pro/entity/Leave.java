package com.connecttrack.pro.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "leaves")
@Data
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // --- LEAVE TYPE ENUM (STRING MAPPING) ---
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type")
    private LeaveType leaveType;

    // --- DATE FIELDS (TEMPORARILY ALLOW NULLS) ---
    @Column(name = "from_date", nullable = true)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = true)
    private LocalDate toDate;

    // --- OTHER FIELDS ---
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    @ManyToOne
    @JoinColumn(name = "approver_id")
    private Employee approver;

    @OneToMany(mappedBy = "leave", cascade = CascadeType.ALL)
    private List<LeaveStatusHistory> history;
}
