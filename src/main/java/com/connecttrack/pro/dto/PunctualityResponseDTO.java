// File: src/main/java/com/connecttrack/pro/dto/PunctualityResponseDTO.java
package com.connecttrack.pro.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PunctualityResponseDTO {

    private List<PunctualityStatDTO> stats;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime officeStartTime;
}