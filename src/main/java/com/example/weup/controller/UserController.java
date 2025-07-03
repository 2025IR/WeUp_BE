package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
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

//TODO. 여기 response entity의 message 부분이 다른 곳이랑 형태가 아예 다름... 수정하기 !
//TODO. ResponseEntity.ok(DataResponseDTO.of()); 형태로 수정하기

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<DataResponseDTO<String>> signUp(@RequestBody SignUpRequestDto signUpRequestDto) {

        userService.signUp(signUpRequestDto);
        
        return ResponseEntity.ok(DataResponseDTO.of("회원가입이 완료되었습니다."));
    }

    @PostMapping("/profile")
    public ResponseEntity<DataResponseDTO<GetProfileResponseDTO>> profile(@LoginUser Long userId) {

        GetProfileResponseDTO getProfileResponseDTO = userService.getProfile(userId);

        return ResponseEntity.ok(DataResponseDTO.of(getProfileResponseDTO, "프로필 조회 완료"));
    }

    //todo. 쿠키 생성 로직 jwtutil로 옮기기
    @PostMapping("/reissuetoken")
    public ResponseEntity<DataResponseDTO<JwtDto>> reissueToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        return userService.reissueToken(refreshToken);
    }

    @PostMapping("/password")
    public ResponseEntity<DataResponseDTO<String>> changePassword(@LoginUser Long userId, @RequestBody PasswordRequestDTO passwordRequestDTO) {

        userService.changePassword(userId, passwordRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("비밀번호가 성공적으로 변경되었습니다."));
    }

    @PutMapping("/profile/edit")
    public ResponseEntity<DataResponseDTO<String>> editProfile(@LoginUser Long userId,
                                                               @ModelAttribute ProfileEditRequestDTO profileEditRequestDTO) throws IOException {

        userService.editProfile(userId, profileEditRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("회원 정보가 성공적으로 수정되었습니다."));
    }


    @PutMapping("/withdraw")
    public ResponseEntity<DataResponseDTO<String>> withdrawUser(@LoginUser Long userId) {

        userService.withdrawUser(userId);

        return ResponseEntity.ok(DataResponseDTO.of("회원 탈퇴가 완료되었습니다."));
    }
}
