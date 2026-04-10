// src\main\java\com\connecttrack\pro\dto\LoginRequest.java
package com.connecttrack.pro.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private String deviceId;
}

