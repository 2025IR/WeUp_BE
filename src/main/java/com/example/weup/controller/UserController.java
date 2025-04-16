package com.example.weup.controller;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.SignUpRequestDto;
import com.example.weup.dto.request.TokenRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetProfileResponseDTO;
import com.example.weup.entity.User;
import com.example.weup.security.JwtDto;
import com.example.weup.security.JwtUtil;
import com.example.weup.repository.UserRepository;
import com.example.weup.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;

    @PostMapping("/signup")
    public ResponseEntity<DataResponseDTO<String>> signUp(@RequestBody SignUpRequestDto signUpRequestDto) {
        userService.signUp(signUpRequestDto);
        String username = signUpRequestDto.getName();
        String message = "회원가입하신 걸 환영합니다, " + username + "님!";
        
        return ResponseEntity.ok(DataResponseDTO.of(message));
    }

    @PostMapping("/profile")
    public ResponseEntity<DataResponseDTO<GetProfileResponseDTO>> profile(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);

        if (token == null || jwtUtil.isExpired(token)) {
            throw new GeneralException(ErrorInfo.UNAUTHORIZED);
        }

        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        GetProfileResponseDTO getProfileResponseDTO = GetProfileResponseDTO.builder()
                .name(user.getName())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .build();

        return ResponseEntity.ok(DataResponseDTO.of(getProfileResponseDTO, "프로필 조회 완료"));
    }

    @PostMapping("/reissuetoken")
    public ResponseEntity<DataResponseDTO<JwtDto>> reissueToken(@RequestBody TokenRequestDTO tokenRequestDTO) {
        Map<String, String> tokens = userService.reissuetoken(tokenRequestDTO);
        String newAccessToken = tokens.get("access_token");
        String newRefreshToken = tokens.get("refresh_token");

        JwtDto jwtDto = JwtDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
        
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + newAccessToken)
                .header("RefreshToken", "Bearer " + newRefreshToken)
                .body(DataResponseDTO.of(jwtDto, "토큰 재발급 완료"));
    }
}
