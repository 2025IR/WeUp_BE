package com.example.weup.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account_social")
public class AccountSocial {

    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void assignUser(User user) {
        this.user = user;
    }

    public void markAsDeleted() {
        this.email = "deleted_" + this.email;
    }
}
