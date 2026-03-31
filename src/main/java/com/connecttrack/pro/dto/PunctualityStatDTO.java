// src\main\java\com\connecttrack\pro\dto\PunctualityStatDTO.java
package com.connecttrack.pro.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@NoArgsConstructor
public class PunctualityStatDTO {

    private DayOfWeek dayOfWeek;
    
    // Add a format annotation to ensure nulls are handled correctly
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime checkInTime;

    private boolean isLate;

    // This constructor is fine
    public PunctualityStatDTO(DayOfWeek dayOfWeek, LocalTime checkInTime) {
        this.dayOfWeek = dayOfWeek;
        this.checkInTime = checkInTime;
        this.isLate = false;
    }
}


