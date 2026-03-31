package com.connecttrack.pro.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "wifi_routers")
public class WiFiRouter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ssid;

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @Column(nullable = false)
    private String location;
}