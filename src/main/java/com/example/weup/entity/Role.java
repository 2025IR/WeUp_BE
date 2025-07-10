package com.example.weup.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "role", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "role_name"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false, updatable = false)
    private Long roleId;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "role_color", nullable = false)
    @Builder.Default
    private String roleColor = "green";

    public void editRole(String name, String color) {
        this.roleName = name;
        this.roleColor = color;
    }

}
