package com.example.jolp.dto;

import com.example.jolp.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;

    private String username;

    private String password;

    private String role;

    private String refreshToken;

    private LocalDateTime passwordExpirationDate;

    public UserDTO(UserEntity user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.refreshToken = user.getRefreshToken();
        this.passwordExpirationDate = user.getPasswordExpirationDate();
    }
}
