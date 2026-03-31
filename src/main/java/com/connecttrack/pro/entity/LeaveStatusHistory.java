package com.connecttrack.pro.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_status_history")
@Data
@NoArgsConstructor
public class LeaveStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_id", nullable = false)
    private Leave leave;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private LeaveStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private LeaveStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_employee_id", nullable = false)
    private Employee changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}