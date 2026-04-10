// src\main\java\com\connecttrack\pro\dto\LoginResponse.java

package com.connecttrack.pro.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate; // <-- NEW IMPORT

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String token;
    private Long id;
    private String fullName;
    private String email;
    private String role;
    private Boolean passwordChangeRequired;
    private Boolean deviceMismatch;

    // --- NEW FIELDS to enrich the user profile ---
    private String departmentName;
    private LocalDate joinDate;
    private String profilePictureUrl;

}