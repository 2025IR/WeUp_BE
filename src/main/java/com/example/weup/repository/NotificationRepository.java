package com.example.weup.repository;

import com.example.weup.entity.Notification;
import com.example.weup.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserOrderByIsReadAscNotificationCreatedAtDesc(User user, Pageable pageable);

    void deleteByIsReadTrueAndNotificationCreatedAtBefore(LocalDateTime threshold);
}