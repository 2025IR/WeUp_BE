package com.example.weup.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "member_role", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "role_id"})
})
@Data
public class Member_Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_role_id", nullable = false, updatable = false)
    private Long memberRoleId;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}