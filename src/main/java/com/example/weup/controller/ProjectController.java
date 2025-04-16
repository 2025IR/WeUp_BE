package com.example.weup.controller;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.ProjectService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final JwtUtil jwtUtil;

    @GetMapping("/{projectId}")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> getProject(@PathVariable Long projectId, @RequestHeader("Authorization") String token) {

        if (token == null || !token.startsWith("Bearer ")) {
            throw new GeneralException(ErrorInfo.UNAUTHORIZED);
        }
        
        String tokenWithoutPrefix = token.substring(7);
        
        try {
            Long userId = jwtUtil.getUserId(tokenWithoutPrefix);
            
            try {
                boolean hasAccess = projectService.hasAccess(userId, projectId);

                if (!hasAccess) {
                    throw new GeneralException(ErrorInfo.FORBIDDEN);
                }

                Map<String, Object> projectInfo = new HashMap<>();
                projectInfo.put("id", projectId);
                
                return ResponseEntity.ok(DataResponseDTO.of(projectInfo, "프로젝트 생성 확인 완료"));
            } catch (GeneralException e) {
                throw e;
            } catch (Exception e) {
                throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
            }
        } catch (JwtException | IllegalArgumentException e) {
            throw new GeneralException(ErrorInfo.UNAUTHORIZED);
        } catch (Exception e) {
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @PostMapping("/testcreate")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> createTestProject(
            @RequestHeader("Authorization") String token) {
        
        if (token == null || !token.startsWith("Bearer ")) {
            throw new GeneralException(ErrorInfo.UNAUTHORIZED);
        }
        
        String tokenWithoutPrefix = token.substring(7);
        
        try {
            Long userId = jwtUtil.getUserId(tokenWithoutPrefix);

            Map<String, Object> result = projectService.createTestProjects(userId);

            return ResponseEntity.ok(DataResponseDTO.of(result, "테스트 프로젝트 생성 완료"));
        } catch (JwtException e) {
            throw new GeneralException(ErrorInfo.UNAUTHORIZED);
        } catch (Exception e) {
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }
}