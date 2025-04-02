package com.example.weup.controller;

import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.JoinDTO;
import com.example.weup.dto.request.TokenRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetProfileResponseDTO;
import com.example.weup.dto.response.TokenResponseDTO;
import com.example.weup.entity.UserEntity;
import com.example.weup.jwt.JWTUtil;
import com.example.weup.repository.UserRepository;
import com.example.weup.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/signup")
    public String signup(@Valid @RequestBody JoinDTO joinDTO) {
        userService.joinProcess(joinDTO);
        String username = joinDTO.getName();
        return "회원가입하신 걸 환영합니다, " + username + "님!";
    }

    @PostMapping("/profile")
    public DataResponseDTO<GetProfileResponseDTO> profile(@RequestHeader("Authorization") String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException(ErrorInfo.UNAUTHORIZED.getMessage("Authorization 헤더가 필요합니다"));
        }

        String accessToken = token.substring(7);
        jwtUtil.validateToken(accessToken);

        Long userId = jwtUtil.getUserId(accessToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(ErrorInfo.USER_NOT_FOUND.getMessage("사용자를 찾을 수 없습니다")));

        return DataResponseDTO.of(new GetProfileResponseDTO(user));
    }

    @PostMapping("/token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRequestDTO request, HttpServletResponse response) {
        TokenResponseDTO tokens = userService.refreshToken(request.getRefreshToken());

        // 토큰을 헤더에 추가
        response.addHeader("Authorization", tokens.getAccessToken());
        response.addHeader("Refresh-Token", tokens.getRefreshToken());

        // 응답 본문에 토큰과 메시지 추가
        HashMap<String, Object> responseBody = new HashMap<>();
        responseBody.put("accessToken", tokens.getAccessToken());
        responseBody.put("refreshToken", tokens.getRefreshToken());

        return ResponseEntity.ok(responseBody);
    }
}
