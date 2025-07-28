package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.request.PasswordRequestDTO;
import com.example.weup.dto.request.ProfileEditRequestDTO;
import com.example.weup.dto.request.RestoreUserRequestDTO;
import com.example.weup.dto.request.SignUpRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetProfileResponseDTO;
import com.example.weup.security.JwtCookieFactory;
import com.example.weup.security.JwtDto;
import com.example.weup.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<DataResponseDTO<String>> signUp(@RequestBody SignUpRequestDTO signUpRequestDto) {

        userService.signUp(signUpRequestDto);
        
        return ResponseEntity.ok(DataResponseDTO.of("회원가입이 완료되었습니다."));
    }

    @PostMapping("/profile")
    public ResponseEntity<DataResponseDTO<GetProfileResponseDTO>> profile(@LoginUser Long userId) {

        GetProfileResponseDTO getProfileResponseDTO = userService.getProfile(userId);

        return ResponseEntity.ok(DataResponseDTO.of(getProfileResponseDTO, "프로필 조회가 완료되었습니다."));
    }

    @PostMapping("/reissuetoken")
    public ResponseEntity<DataResponseDTO<JwtDto>> reissueToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        JwtDto jwtDto = userService.reissueToken(refreshToken);
        ResponseCookie cookie = JwtCookieFactory.createRefreshCookie(jwtDto.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(DataResponseDTO.of(jwtDto.withoutRefreshToken(), "토큰 재발급이 완료되었습니다."));
    }

    @PostMapping("/password")
    public ResponseEntity<DataResponseDTO<String>> changePassword(@LoginUser Long userId, @RequestBody PasswordRequestDTO passwordRequestDTO) {

        userService.changePassword(userId, passwordRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("비밀번호가 성공적으로 변경되었습니다."));
    }

    @PutMapping("/profile/edit")
    public ResponseEntity<DataResponseDTO<String>> editProfile(@LoginUser Long userId,
                                                               @Valid @ModelAttribute ProfileEditRequestDTO profileEditRequestDTO) throws IOException {

        userService.editProfile(userId, profileEditRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("회원 정보가 성공적으로 수정되었습니다."));
    }


    @PutMapping("/withdraw")
    public ResponseEntity<DataResponseDTO<String>> withdrawUser(@LoginUser Long userId) {

        userService.withdrawUser(userId);

        return ResponseEntity.ok(DataResponseDTO.of("회원 탈퇴가 완료되었습니다."));
    }

    @PostMapping("/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataResponseDTO<String>> restoreWithdrawnUser(@RequestBody RestoreUserRequestDTO restoreUserRequestDTO) {
        userService.restoreWithdrawnUser(restoreUserRequestDTO);
        return ResponseEntity.ok(DataResponseDTO.of("유저 복구가 완료되었습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<DataResponseDTO<String>> logout(
            @LoginUser Long userId,
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        userService.logout(userId, refreshToken);

        ResponseCookie deleteCookie = JwtCookieFactory.deleteRefreshCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(DataResponseDTO.of("로그아웃이 완료되었습니다."));
    }
}
