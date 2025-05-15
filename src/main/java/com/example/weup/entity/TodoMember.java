package com.example.weup.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "todo_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"todo_id", "member_id"})
})
public class TodoMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;
}
