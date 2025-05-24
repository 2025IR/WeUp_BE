package com.example.weup.repository;

import com.example.weup.entity.TodoMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoMemberRepository extends JpaRepository<TodoMember, Long> {
    void deleteByTodo_TodoId(Long todoId);

    void deleteAllByTodo_TodoId(Long todoId);
}
