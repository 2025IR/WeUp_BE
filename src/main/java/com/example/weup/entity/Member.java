package com.example.weup.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "member", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "project_id"})
})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonBackReference
    private Project project;

    @Column(name = "available_time", length = 252, nullable = false)
    @Builder.Default
    private String availableTime = "0".repeat(252);

    @Column(name = "is_member_deleted", nullable = false)
    @Builder.Default
    private boolean isMemberDeleted = false;

    @Column(name = "last_access_time", nullable = false)
    private LocalDateTime lastAccessTime;

    @Column(name = "is_leader", nullable = false)
    @Builder.Default
    private boolean isLeader = false;

    public void promoteToLeader() {
        this.isLeader = true;
    }

    public void demoteFromLeader() {
        this.isLeader = false;
    }

    public void markAsDeleted() {
        this.isMemberDeleted = true;
    }

    public void editSchedule(String availableTime) {
        this.availableTime = availableTime;
    }

    public void reJoin() {
        this.isMemberDeleted = false;
        this.lastAccessTime = LocalDateTime.now();
    }

    public void assignDeletedUser(User user) {
        this.user = user;
    }
}