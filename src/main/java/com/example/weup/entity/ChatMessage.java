package com.example.weup.entity;

import com.example.weup.constant.DisplayType;
import com.example.weup.constant.SenderType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long messageId;

    @Column(nullable = false, updatable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member member;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    @Builder.Default
    @JsonProperty("isImage")
    private Boolean isImage = false;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private SenderType senderType = SenderType.MEMBER;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private DisplayType displayType = DisplayType.DEFAULT;

    private String originalMessage;

    private String originalSenderName;

    public void changeSenderToWithdraw() {
        this.member = null;
        this.senderType = SenderType.WITHDRAW;
    }
}
