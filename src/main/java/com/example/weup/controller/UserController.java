package com.example.weup.controller;

import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.SignInRequestDto;
import com.example.weup.dto.request.SignUpRequestDto;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetProfileResponseDTO;
import com.example.weup.entity.User;
import com.example.weup.security.JwtUtil;
import com.example.weup.security.JwtDto;
import com.example.weup.repository.UserRepository;
import com.example.weup.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;

    @PostMapping("/signup")
    public String signUp(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {

        userService.signUp(signUpRequestDto);
        String username = signUpRequestDto.getName();

        return "회원가입하신 걸 환영합니다, " + username + "님!";  // 이 String 값을 ResponseEntity에 담아서
    }

//    // Login Logic
//    @PostMapping("/signIn")
//    public ResponseEntity<?> signIn(@RequestBody SignInRequestDto signInRequestDto) {
//
//        JwtDto jwtDto = userService.signIn(signInRequestDto);
//
//        return new ResponseEntity<>(jwtDto.getAccessToken(), HttpStatus.OK);
//    }

    @PostMapping("/profile")
    public ResponseEntity<GetProfileResponseDTO> profile(HttpServletRequest request) {
//        if (token == null || !token.startsWith("Bearer ")) {
//            throw new RuntimeException(ErrorInfo.UNAUTHORIZED.getMessage("Authorization 헤더가 필요합니다"));
//        }
//
//        String accessToken = token.substring(7);
        String token = jwtUtil.resolveToken(request);

        if (token == null || jwtUtil.isExpired(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(ErrorInfo.USER_NOT_FOUND.getMessage("사용자를 찾을 수 없습니다")));  //RuntimeException?

        GetProfileResponseDTO getProfileResponseDTO = GetProfileResponseDTO.builder()
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .profileImage(user.getProfileImage())
                .build();

        return ResponseEntity.ok(getProfileResponseDTO);
    }

//    @PostMapping("/token")
//    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRequestDTO request, HttpServletResponse response) {
//        TokenResponseDTO tokens = userService.refreshToken(request.getRefreshToken());
//
//        // 토큰을 헤더에 추가
//        response.addHeader("Authorization", tokens.getAccessToken());
//        response.addHeader("Refresh-Token", tokens.getRefreshToken());
//
//        // 응답 본문에 토큰과 메시지 추가
//        HashMap<String, Object> responseBody = new HashMap<>();
//        responseBody.put("accessToken", tokens.getAccessToken());
//        responseBody.put("refreshToken", tokens.getRefreshToken());
//
//        return ResponseEntity.ok(responseBody);
//    }

}
