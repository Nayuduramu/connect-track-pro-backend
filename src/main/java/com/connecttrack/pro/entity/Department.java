// src\main\java\com\connecttrack\pro\entity\Department.java
package com.connecttrack.pro.entity;
import jakarta.persistence.*;
import lombok.Data;
@Data @Entity @Table(name = "departments")
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String name;
}