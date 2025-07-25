package com.example.weup.entity;

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
    @Column(name = "message_id", nullable = false, updatable = false)
    private Long messageId;

    @ManyToOne
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne
    @JoinColumn(name = "chat_room_member_id")
    private User user; // TODO. 수정

    @Column
    private String message;

    @Column
    @Builder.Default
    @JsonProperty("isImage")
    private Boolean isImage = false;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

//    public void changeSender(Member member){
//        this.member = member;
//    }
}
