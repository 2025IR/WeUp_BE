package com.example.weup.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id", nullable = false, updatable = false)
    private Long chatRoomId;

    @Column(nullable = false)
    private String chatRoomName;

    @OneToOne(fetch = FetchType.EAGER)
    private Project project;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean basic = false;

    public void editChatRoomName(String chatRoomName) {
        this.chatRoomName = chatRoomName;
    }
}
