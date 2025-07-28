package com.example.weup.entity;

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

    @ManyToOne
    @JoinColumn
    private ChatRoom chatRoom;

    @ManyToOne
    @JoinColumn
    private Member senderId;

    @Column
    private String message;

    @Column
    @Builder.Default
    @JsonProperty("isImage")
    private Boolean isImage = false;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private SenderType senderType;

    public void changeSender(Member member){
        this.member = member;
    }
}
