package com.example.weup.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "member", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "project_id"})
})
@Data
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String role = "MEMBER";

    @Column(nullable = false)
    private boolean isMemberDeleted = false;
} 