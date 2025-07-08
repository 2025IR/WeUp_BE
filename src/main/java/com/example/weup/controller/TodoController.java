package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.request.*;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.TodoListResponseDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.TodoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/todo")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping("/create")
    public ResponseEntity<DataResponseDTO<String>> createTodo(@LoginUser Long userId,
                                                              @RequestBody CreateTodoRequestDTO createTodoRequestDTO) {

        todoService.createTodo(userId, createTodoRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("투두 생성이 완료되었습니다."));
    }

    @PostMapping("/list/{projectId}")
    public ResponseEntity<DataResponseDTO<List<TodoListResponseDTO>>> getTodoList(@LoginUser Long userId,
                                                                                  @PathVariable Long projectId) {

        List<TodoListResponseDTO> todos = todoService.getTodoList(userId, projectId);

        return ResponseEntity.ok(DataResponseDTO.of(todos, "투두 목록 조회 완료가 완료되었습니다."));
    }

    @PutMapping("/edit")
    public ResponseEntity<DataResponseDTO<String>> editTodo(@LoginUser Long userId,
                                                            @RequestBody EditTodoRequestDTO editTodoRequestDTO) {

        todoService.editTodo(userId, editTodoRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("투두 수정이 완료되었습니다."));
    }

    @PutMapping("/state")
    public ResponseEntity<DataResponseDTO<String>> editTodoStatus(@LoginUser Long userId,
                                                                  @RequestBody EditTodoStatusRequestDTO editTodoStatusRequestDTO) {

        todoService.editTodoStatus(userId, editTodoStatusRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("투두 상태 수정이 완료되었습니다."));
    }

    @DeleteMapping("/delete/{todoId}")
    public ResponseEntity<DataResponseDTO<String>> deleteTodo(@LoginUser Long userId,
                                                              @PathVariable Long todoId) {

        todoService.deleteTodo(userId, todoId);

        return ResponseEntity.ok(DataResponseDTO.of("투두 삭제가 완료되었습니다."));
    }
}
