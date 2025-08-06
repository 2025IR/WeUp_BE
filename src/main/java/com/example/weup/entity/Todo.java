package com.example.weup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "todo")
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    @Builder.Default
    private String todoName = "";

    @Column(nullable = false)
    @Builder.Default
    private LocalDate startDate = LocalDate.now();

    @Column
    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    private Byte todoStatus = 0;

    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TodoMember> todoMembers = new ArrayList<>();
}
