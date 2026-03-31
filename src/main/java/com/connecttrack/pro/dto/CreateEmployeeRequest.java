package com.connecttrack.pro.dto;
import lombok.Data;
@Data
public class CreateEmployeeRequest {
    private String fullName;
    private String email;
    private String password; // Admin sets an initial temporary password
    private String role;
    private Long departmentId;
}