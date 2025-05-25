package com.example.weup.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.C;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id", nullable = false, updatable = false)
    private Long messageId;

    @ManyToOne
    private ChatRoom chatRoom;

    @ManyToOne
    private User user;

    @Column
    private String message;

    @Column(name = "is_image")
    @Builder.Default
    private Boolean isImage = false;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
