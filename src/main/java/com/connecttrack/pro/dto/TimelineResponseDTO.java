// File: src/main/java/com/connecttrack/pro/dto/TimelineResponseDTO.java
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
public class TimelineResponseDTO {

    private List<TimelinePeriodDTO> periods;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime officeStartTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime officeEndTime;
}