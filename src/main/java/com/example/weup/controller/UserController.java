package com.example.weup.controller;

import com.example.weup.dto.request.PasswordRequestDTO;
import com.example.weup.dto.request.ProfileEditRequestDTO;
import com.example.weup.dto.request.SignUpRequestDto;
import com.example.weup.dto.request.TokenRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetProfileResponseDTO;
import com.example.weup.security.JwtDto;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final JwtUtil jwtUtil;

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

        GetProfileResponseDTO getProfileResponseDTO = userService.getProfile(token);

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
                .body(DataResponseDTO.of(jwtDto, "토큰 재발급 완료"));
    }

    @PostMapping("/reissue")
    public ResponseEntity<DataResponseDTO<JwtDto>> reissue(@RequestParam Long userId) {
        Map<String, String> tokens = userService.reissue(userId);
        String newAccessToken = tokens.get("access_token");

        JwtDto jwtDto = JwtDto.builder()
                .accessToken(newAccessToken)
                .build();

        return ResponseEntity.ok()
                .body(DataResponseDTO.of(jwtDto, "토큰 재발급 완료 - 임시 API"));
    }

    @PostMapping("/password")
    public ResponseEntity<DataResponseDTO<String>> changePassword(HttpServletRequest request, @RequestBody PasswordRequestDTO passwordRequestDTO) {
        String token = jwtUtil.resolveToken(request);
        userService.changePassword(token, passwordRequestDTO);
        String message = "비밀번호가 성공적으로 변경되었습니다.";
        return ResponseEntity.ok(DataResponseDTO.of(message));
    }

    @PutMapping("/profile/edit")
    public ResponseEntity<DataResponseDTO<String>> editProfile(
            HttpServletRequest request,
            @ModelAttribute ProfileEditRequestDTO profileEditRequestDTO) throws IOException {

        String token = jwtUtil.resolveToken(request);
        userService.editProfile(
                token,
                profileEditRequestDTO.getName(),
                profileEditRequestDTO.getPhoneNumber(),
                profileEditRequestDTO.getProfileImage());

        return ResponseEntity.ok(DataResponseDTO.of("회원 정보가 성공적으로 수정되었습니다."));
    }


    @PutMapping("/withdraw")
    public ResponseEntity<DataResponseDTO<String>> withdrawUser(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        userService.withdrawUser(token);
        String message = "회원 탈퇴가 완료되었습니다.";
        return ResponseEntity.ok(DataResponseDTO.of(message));
    }
}
