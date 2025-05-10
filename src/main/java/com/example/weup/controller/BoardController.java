package com.example.weup.controller;

import com.example.weup.dto.request.*;
import com.example.weup.dto.response.*;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final JwtUtil jwtUtil;

    @PostMapping(value = "/create")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> createBoard(
            HttpServletRequest request,
            @RequestParam Long projectId,
            @RequestParam String title,
            @RequestParam String contents,
            @RequestParam String tag,
            @RequestPart(value = "file", required = false) List<MultipartFile> files) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = boardService.createBoard(
                userId,
                projectId,
                title,
                contents,
                tag,
                files
        );

        return ResponseEntity.ok(DataResponseDTO.of(result, "게시글 작성이 완료되었습니다."));
    }

    @PostMapping("/list")
    public ResponseEntity<DataResponseDTO<Page<BoardListResponseDTO>>> getBoardList(
            HttpServletRequest request,
            @RequestBody BoardListRequestDTO boardListRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Pageable pageable = PageRequest.of(
                boardListRequestDTO.getPage(),
                boardListRequestDTO.getSize(),
                Sort.by(Sort.Direction.DESC, "boardCreateTime")
        );

        Page<BoardListResponseDTO> boards = boardService.getBoardList(
                userId,
                boardListRequestDTO.getProjectId(),
                boardListRequestDTO.getTag(),
                boardListRequestDTO.getSearch(),
                pageable
        );

        return ResponseEntity.ok(DataResponseDTO.of(boards, "게시글 조회가 완료되었습니다."));
    }

    @PostMapping("/detail")
    public ResponseEntity<DataResponseDTO<BoardDetailResponseDTO>> getBoardDetail(
            HttpServletRequest request,
            @RequestBody BoardDetailRequestDTO boardDetailRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        BoardDetailResponseDTO result = boardService.getBoardDetail(
                userId,
                boardDetailRequestDTO.getBoardId(),
                boardDetailRequestDTO.getProjectId()
                );
        return ResponseEntity.ok(DataResponseDTO.of(result, "게시글 열람이 완료되었습니다."));
    }

    @PutMapping("/edit")
    public ResponseEntity<DataResponseDTO<String>> editBoard(
            @RequestParam Long projectId,
            @RequestParam Long boardId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String contents,
            @RequestPart(required = false) MultipartFile file,
            HttpServletRequest request
    ) throws IOException {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        boardService.editBoard(userId, projectId, boardId, title, tag, contents, file);

        return ResponseEntity.ok(DataResponseDTO.of("게시글 수정 완료: " + boardId));
    }


    @DeleteMapping("/delete")
    public ResponseEntity<DataResponseDTO<String>> deleteBoard(
            HttpServletRequest request,
            @RequestBody DeleteBoardRequestDTO deleteBoardRequestDTO) {

        Long userId = jwtUtil.getUserId(jwtUtil.resolveToken(request));
        boardService.deleteBoard(
                userId,
                deleteBoardRequestDTO.getBoardId(),
                deleteBoardRequestDTO.getProjectId()
                );

        return ResponseEntity.ok(DataResponseDTO.of("게시글이 삭제되었습니다."));
    }
}