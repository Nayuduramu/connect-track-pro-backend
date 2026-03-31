package com.connecttrack.pro.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "holiday") // <-- THIS IS THE CRITICAL FIX
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private LocalDate holidayDate;

    // --- NEW FIELD ---
    @Enumerated(EnumType.STRING)
    private HolidayType holidayType;

    // --- NEW ENUM ---
    public enum HolidayType {
        NATIONAL,
        WEEKEND_OVERRIDE
    }
}