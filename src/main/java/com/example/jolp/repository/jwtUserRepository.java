package com.example.jolp.repository;

import com.example.jolp.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface jwtUserRepository extends JpaRepository<UserEntity, Long> {
    Boolean existsByUsername(String username);
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByRefreshToken(String refreshToken);
}