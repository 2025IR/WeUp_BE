package com.example.weup.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(name = "profile_image", nullable = false)
    private String profileImage;

    @Column(name = "phone_number")
    @Builder.Default
    private String phoneNumber = "-";

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    @Builder.Default
    private byte isDarkMode = 1;

    @Column(name = "is_user_withdrawal", nullable = false)
    @Builder.Default
    private boolean isUserWithdrawal = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "refresh_token")
    private String refreshToken;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private AccountSocial accountSocial;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role));
    }

    @Override
    public String getUsername() {
        return accountSocial.getEmail();
    }

    @Override
    public String getPassword() {
        return accountSocial.getPassword();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !isUserWithdrawal;
    }

    public void withdraw() {
        this.isUserWithdrawal = true;
        this.deletedAt = LocalDateTime.now();
        this.refreshToken = null;
    }

    public void restore() {
        this.isUserWithdrawal = false;
        this.deletedAt = null;
    }

    public void linkAccount(AccountSocial accountSocial) {
        this.accountSocial = accountSocial;
        accountSocial.assignUser(this);
    }

    public User editName(String name) {
        this.name = name;
        return this;
    }

    public User editPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
