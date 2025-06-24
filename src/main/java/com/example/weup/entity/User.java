package com.example.weup.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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
    @Builder.Default
    private String profileImage = "0114fa55-f353-41bd-a6d1-4dff2f762f6c-smiley3.png";

    @Column(name = "phone_number")
    @Builder.Default
    private String phoneNumber = "-";

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    @Builder.Default
    private byte isDarkMode = 1;

    @Column(name = "is_user_withdrawal", nullable = false)
    private boolean isUserWithdrawal;

    @Column(name = "refresh_token")
    private String refreshToken;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
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
}