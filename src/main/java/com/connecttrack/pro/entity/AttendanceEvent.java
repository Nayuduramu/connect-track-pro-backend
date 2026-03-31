package com.connecttrack.pro.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data @Entity @Table(name = "attendance_events") @NoArgsConstructor
public class AttendanceEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @Column(name = "router_mac") private String routerMac;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false) private EventType eventType;
    @Column(name = "event_timestamp", nullable = false) private LocalDateTime eventTimestamp;
    public enum EventType { CONNECT, DISCONNECT }
    public AttendanceEvent(Employee employee, String routerMac, EventType eventType, LocalDateTime eventTimestamp) {
        this.employee = employee; this.routerMac = routerMac; this.eventType = eventType; this.eventTimestamp = eventTimestamp;
    }
    private Long sessionDurationMinutes;
}