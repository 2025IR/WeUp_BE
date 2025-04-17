package com.example.weup.entity;

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
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId // PK를 공유하며 외래키도 된다.
    @JoinColumn(name = "user_id") // 이 필드에 외래키가 들어간다.
    private User user;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;
}
