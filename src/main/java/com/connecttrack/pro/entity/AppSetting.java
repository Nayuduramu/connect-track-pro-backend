package com.connecttrack.pro.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
// --- CONFIRM THIS LINE ---
// It MUST point to the plural 'app_settings'.
@Table(name = "app_settings") 
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppSetting {
    @Id
    private String settingKey;
    private String settingValue;
}