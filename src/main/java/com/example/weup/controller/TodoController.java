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
import java.util.Map;

@RestController
@RequestMapping("/todo")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    private final JwtUtil jwtUtil;

    @PostMapping("/create")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> createTodo(
            HttpServletRequest request,
            @RequestBody CreateTodoRequestDTO createTodoRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = todoService.createTodo(
                userId,
                createTodoRequestDTO.getProjectId(),
                createTodoRequestDTO.getMemberIds(),
                createTodoRequestDTO.getTodoName(),
                createTodoRequestDTO.getStartDate(),
                createTodoRequestDTO.getEndDate()
        );

        return ResponseEntity.ok(DataResponseDTO.of(result, "투두 생성 완료"));
    }

    @PostMapping("/list")
    public ResponseEntity<DataResponseDTO<List<TodoListResponseDTO>>> getTodoList(
            HttpServletRequest request,
            @RequestBody TodoListRequestDTO todoListRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        List<TodoListResponseDTO> todos = todoService.getTodoList(
                userId,
                todoListRequestDTO.getProjectId()
        );

        return ResponseEntity.ok(DataResponseDTO.of(todos, "투두 목록 조회 완료"));
    }

    @PutMapping("/edit")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> editTodo(
            HttpServletRequest request,
            @RequestBody EditTodoRequestDTO editTodoRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = todoService.editTodo(
                userId,
                editTodoRequestDTO.getProjectId(),
                editTodoRequestDTO.getTodoId(),
                editTodoRequestDTO.getMemberIds(),
                editTodoRequestDTO.getTodoName(),
                editTodoRequestDTO.getStartDate(),
                editTodoRequestDTO.getEndDate()
        );

        return ResponseEntity.ok(DataResponseDTO.of(result, "투두 수정 완료"));
    }

    @PutMapping("/state")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> editTodoStatus(
            HttpServletRequest request,
            @RequestBody EditTodoStatusRequestDTO editTodoStatusRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = todoService.editTodoStatus(
                userId,
                editTodoStatusRequestDTO.getProjectId(),
                editTodoStatusRequestDTO.getTodoId(),
                editTodoStatusRequestDTO.getStatus()
        );

        return ResponseEntity.ok(DataResponseDTO.of(result, "투두 상태 수정 완료"));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<DataResponseDTO<String>> deleteTodo(
            HttpServletRequest request,
            @RequestBody DeleteTodoRequestDTO deleteTodoRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        todoService.deleteTodo(
                userId,
                deleteTodoRequestDTO.getProjectId(),
                deleteTodoRequestDTO.getTodoId()
                );

        return ResponseEntity.ok(DataResponseDTO.of("투두 삭제 완료"));
    }

}
