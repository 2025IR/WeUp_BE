package com.example.weup.entity;


import com.example.weup.constant.SenderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "board")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id", nullable = false, updatable = false)
    private Long boardId;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "title")
    private String title;

    @Column(name= "board_create_time")
    @Builder.Default
    private LocalDateTime boardCreateTime = LocalDateTime.now();

    @Column(name= "contents")
    private String contents;

    @Column(name= "sender_type")
    @Builder.Default
    private SenderType senderType = SenderType.MEMBER;

    public void editBoard(String title, String contents, Tag tag) {
        this.title = title;
        this.contents = contents;
        this.tag = tag;
    }

}