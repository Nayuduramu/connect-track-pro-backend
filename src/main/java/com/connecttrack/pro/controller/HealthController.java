package com.connecttrack.pro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "Connect Track Pro API is running";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}