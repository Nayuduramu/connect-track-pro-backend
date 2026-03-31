package com.connecttrack.pro.dto;

import lombok.Data;

@Data
public class EditEmployeeRequest {
    private String fullName;
    private String email;
    private String role;
    private Long departmentId;
}