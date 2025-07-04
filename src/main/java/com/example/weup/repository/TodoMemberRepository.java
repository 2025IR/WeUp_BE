package com.example.weup.repository;

import com.example.weup.entity.Member;
import com.example.weup.entity.TodoMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoMemberRepository extends JpaRepository<TodoMember, Long> {
    void deleteByTodo_TodoId(Long todoId);

    void deleteAllByTodo_TodoId(Long todoId);

    List<TodoMember> findAllByTodo_TodoId(Long todoId);

    void deleteByMember(Member member);

}
