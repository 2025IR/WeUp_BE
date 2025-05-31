package com.example.weup.controller;

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
    private final JwtUtil jwtUtil;

    @PostMapping("/create")
    public ResponseEntity<DataResponseDTO<String>> createTodo(
            HttpServletRequest request,
            @RequestBody CreateTodoRequestDTO createTodoRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        todoService.createTodo(
                userId,
                createTodoRequestDTO
        );

        return ResponseEntity.ok(DataResponseDTO.of("투두 생성 완료"));
    }

    @PostMapping("/list/{projectId}")
    public ResponseEntity<DataResponseDTO<List<TodoListResponseDTO>>> getTodoList(
        HttpServletRequest request,
        @PathVariable Long projectId) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        List<TodoListResponseDTO> todos = todoService.getTodoList(
                userId,
                projectId
        );

        return ResponseEntity.ok(DataResponseDTO.of(todos, "투두 목록 조회 완료"));
    }

    @PutMapping("/edit")
    public ResponseEntity<DataResponseDTO<String>> editTodo(
            HttpServletRequest request,
            @RequestBody EditTodoRequestDTO editTodoRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        todoService.editTodo(
                userId,
                editTodoRequestDTO
        );

        return ResponseEntity.ok(DataResponseDTO.of("투두 수정 완료"));
    }

    @PutMapping("/state")
    public ResponseEntity<DataResponseDTO<String>> editTodoStatus(
            HttpServletRequest request, @RequestBody EditTodoStatusRequestDTO editTodoStatusRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        System.out.println("투두 상태 변경 컨트롤러 : " + editTodoStatusRequestDTO.getTodoId() + editTodoStatusRequestDTO.getStatus());

        todoService.editTodoStatus(
                userId,
                editTodoStatusRequestDTO
        );

        return ResponseEntity.ok(DataResponseDTO.of("투두 상태 수정 완료"));
    }

    @DeleteMapping("/delete/{todoId}")
    public ResponseEntity<DataResponseDTO<String>> deleteTodo(
            HttpServletRequest request,
            @PathVariable Long todoId) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        todoService.deleteTodo(
                userId,
                todoId
                );

        return ResponseEntity.ok(DataResponseDTO.of("투두 삭제 완료"));
    }
}
