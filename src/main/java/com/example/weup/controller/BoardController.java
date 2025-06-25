package com.example.weup.controller;

import com.example.weup.dto.request.*;
import com.example.weup.dto.response.*;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final JwtUtil jwtUtil;

    @PostMapping("/create")
    public ResponseEntity<DataResponseDTO<String>> createBoard(
            HttpServletRequest request,
            @ModelAttribute BoardCreateRequestDTO boardCreateRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        boardService.createBoard(
                userId,
                boardCreateRequestDTO
        );

        return ResponseEntity.ok(DataResponseDTO.of("게시글 작성이 완료되었습니다."));
    }

    @PostMapping("/list/{projectId}")
    public ResponseEntity<DataResponseDTO<Page<BoardListResponseDTO>>> getBoardList(
            HttpServletRequest request,
            @PathVariable Long projectId,
            @RequestBody BoardListRequestDTO boardListRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Page<BoardListResponseDTO> boards = boardService.getBoardList(userId, projectId, boardListRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of(boards, "게시글 조회가 완료되었습니다."));
    }

    @PostMapping("/detail/{boardId}")
    public ResponseEntity<DataResponseDTO<BoardDetailResponseDTO>> getBoardDetail(
            HttpServletRequest request,
            @PathVariable Long boardId) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        BoardDetailResponseDTO result = boardService.getBoardDetail(
                userId,
                boardId
                );
        return ResponseEntity.ok(DataResponseDTO.of(result, "게시글 열람이 완료되었습니다."));
    }

    @PutMapping("/edit/{boardId}")
    public ResponseEntity<DataResponseDTO<String>> editBoard(
            HttpServletRequest request,
            @PathVariable Long boardId,
            @ModelAttribute EditBoardRequestDTO editBoardRequestDTO
    ) throws IOException {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        boardService.editBoard(
                userId,
                boardId,
                editBoardRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("게시글 수정이 완료되었습니다."));
    }

    @DeleteMapping("/delete/{boardId}")
    public ResponseEntity<DataResponseDTO<String>> deleteBoard(
            HttpServletRequest request,
            @PathVariable Long boardId) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        boardService.deleteBoard(
                userId,
                boardId
                );

        return ResponseEntity.ok(DataResponseDTO.of("게시글이 삭제되었습니다."));
    }
}