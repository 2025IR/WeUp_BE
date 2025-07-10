package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.request.*;
import com.example.weup.dto.response.*;
import com.example.weup.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @PostMapping("/create")
    public ResponseEntity<DataResponseDTO<String>> createBoard(
            @LoginUser Long userId,
            @Valid @ModelAttribute BoardCreateRequestDTO boardCreateRequestDTO) {

        boardService.createBoard(userId, boardCreateRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("게시글 작성이 완료되었습니다."));
    }

    @PostMapping("/list/{projectId}")
    public ResponseEntity<DataResponseDTO<Page<BoardListResponseDTO>>> getBoardList(
            @LoginUser Long userId,
            @PathVariable Long projectId,
            @RequestBody BoardListRequestDTO boardListRequestDTO) {

        Page<BoardListResponseDTO> boards = boardService.getBoardList(userId, projectId, boardListRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of(boards, "게시글 조회가 완료되었습니다."));
    }

    @PostMapping("/detail/{boardId}")
    public ResponseEntity<DataResponseDTO<BoardDetailResponseDTO>> getBoardDetail(
            @LoginUser Long userId,
            @PathVariable Long boardId) {

        BoardDetailResponseDTO result = boardService.getBoardDetail(userId, boardId);

        return ResponseEntity.ok(DataResponseDTO.of(result, "게시글 열람이 완료되었습니다."));
    }

    @PutMapping("/edit/{boardId}")
    public ResponseEntity<DataResponseDTO<String>> editBoard(
            @LoginUser Long userId,
            @PathVariable Long boardId,
            @Valid @ModelAttribute EditBoardRequestDTO editBoardRequestDTO) {

        boardService.editBoard(userId, boardId, editBoardRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("게시글 수정이 완료되었습니다."));
    }

    @DeleteMapping("/delete/{boardId}")
    public ResponseEntity<DataResponseDTO<String>> deleteBoard(
            @LoginUser Long userId,
            @PathVariable Long boardId) {

        boardService.deleteBoard(userId, boardId);

        return ResponseEntity.ok(DataResponseDTO.of("게시글이 삭제되었습니다."));
    }
}