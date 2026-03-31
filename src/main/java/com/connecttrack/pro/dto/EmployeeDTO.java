package com.connecttrack.pro.dto;

public class EmployeeDTO {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String departmentName;
    private String status;
    private String deviceId;
    private String customStartTime;
    private String customEndTime;

    // ======================================================
    // CUSTOM LOCATION FIELDS
    // ======================================================
    private Double officeLatitude;
    private Double officeLongitude;
    private Double officeRadius;

    private DepartmentDTO department;

    // ======================================================
    // NESTED DEPARTMENT DTO
    // ======================================================
    public static class DepartmentDTO {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // ======================================================
    // GETTERS & SETTERS
    // ======================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getCustomStartTime() { return customStartTime; }
    public void setCustomStartTime(String customStartTime) {
        this.customStartTime = customStartTime;
    }

    public String getCustomEndTime() { return customEndTime; }
    public void setCustomEndTime(String customEndTime) {
        this.customEndTime = customEndTime;
    }

    public DepartmentDTO getDepartment() { return department; }
    public void setDepartment(DepartmentDTO department) {
        this.department = department;
    }

    // ======================================================
    // LOCATION GETTERS & SETTERS
    // ======================================================

    public Double getOfficeLatitude() { return officeLatitude; }
    public void setOfficeLatitude(Double officeLatitude) {
        this.officeLatitude = officeLatitude;
    }

    public Double getOfficeLongitude() { return officeLongitude; }
    public void setOfficeLongitude(Double officeLongitude) {
        this.officeLongitude = officeLongitude;
    }

    public Double getOfficeRadius() { return officeRadius; }
    public void setOfficeRadius(Double officeRadius) {
        this.officeRadius = officeRadius;
    }
}
