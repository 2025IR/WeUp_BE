package com.example.weup.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "jwt_project")
@Data
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

} 