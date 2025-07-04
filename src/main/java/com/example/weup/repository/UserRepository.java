package com.example.weup.repository;

import com.example.weup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByAccountSocialEmail(String email);

    Optional<User> findByAccountSocialEmail(String email);

    List<User> findAllByDeletedAtBefore(LocalDateTime threshold);
}