package com.connecttrack.pro.mapper;

import com.connecttrack.pro.dto.EmployeeDTO;
import com.connecttrack.pro.entity.Employee;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class EmployeeMapper {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public EmployeeDTO toDTO(Employee employee) {

        if (employee == null) {
            return null;
        }

        EmployeeDTO dto = new EmployeeDTO();

        // ======================================================
        // BASIC DETAILS
        // ======================================================
        dto.setId(employee.getId());
        dto.setFullName(employee.getFullName());
        dto.setEmail(employee.getEmail());

        if (employee.getRole() != null) {
            dto.setRole(employee.getRole().getName());
        }

        // ======================================================
        // DEPARTMENT
        // ======================================================
        if (employee.getDepartment() != null) {

            EmployeeDTO.DepartmentDTO departmentDTO =
                    new EmployeeDTO.DepartmentDTO();

            departmentDTO.setId(employee.getDepartment().getId());
            departmentDTO.setName(employee.getDepartment().getName());

            dto.setDepartment(departmentDTO);
            dto.setDepartmentName(employee.getDepartment().getName());

        } else {
            dto.setDepartmentName("Unassigned");
        }

        // ======================================================
        // STATUS
        // ======================================================
        if (employee.getStatus() != null) {
            dto.setStatus(employee.getStatus().name());
        } else {
            dto.setStatus("INACTIVE");
        }

        // ======================================================
        // CUSTOM OFFICE TIMINGS
        // ======================================================
        if (employee.getCustomStartTime() != null) {
            dto.setCustomStartTime(
                    employee.getCustomStartTime().format(TIME_FORMATTER)
            );
        }

        if (employee.getCustomEndTime() != null) {
            dto.setCustomEndTime(
                    employee.getCustomEndTime().format(TIME_FORMATTER)
            );
        }

        dto.setDeviceId(employee.getDeviceId());

        // ======================================================
        // CUSTOM LOCATION MAPPING
        // ======================================================
        dto.setOfficeLatitude(employee.getOfficeLatitude());
        dto.setOfficeLongitude(employee.getOfficeLongitude());

        // If radius is null, default to 100.0 (safe fallback)
        dto.setOfficeRadius(
                employee.getOfficeRadius() != null
                        ? employee.getOfficeRadius()
                        : 100.0
        );

        return dto;
    }
}
