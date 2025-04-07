package com.example.weup.controller;

import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.ProjectService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final JwtUtil jwtUtil;

    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProject(@PathVariable Long projectId, @RequestHeader("Authorization") String token) {
        
        // 토큰 형식 확인 및 Bearer 제거
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다");
        }
        
        String tokenWithoutPrefix = token.substring(7);
        
        try {
            Long userId = jwtUtil.getUserId(tokenWithoutPrefix);
            
            try {
                boolean hasAccess = projectService.hasAccess(userId, projectId);

                if (!hasAccess) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 프로젝트에 접근 권한이 없습니다");
                }

                Map<String, Object> projectInfo = new HashMap<>();
                projectInfo.put("id", projectId);
                
                return ResponseEntity.ok(DataResponseDTO.of(projectInfo));
            } catch (ResponseStatusException e) {
                throw e;
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트 접근 확인 중 오류가 발생했습니다");
            }
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");
        }
    }

    @PostMapping("/testcreate")
    public ResponseEntity<?> createTestProject(
            @RequestHeader("Authorization") String token) {
        
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다");
        }
        
        String tokenWithoutPrefix = token.substring(7);
        
        try {
            Long userId = jwtUtil.getUserId(tokenWithoutPrefix);

            Map<String, Object> result = projectService.createTestProjects(userId);

            return ResponseEntity.ok(DataResponseDTO.of(result));
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "테스트 프로젝트 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 