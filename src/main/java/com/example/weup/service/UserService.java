package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.SignUpRequestDto;
import com.example.weup.dto.request.TokenRequestDTO;
import com.example.weup.dto.request.PasswordRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetProfileResponseDTO;
import com.example.weup.entity.AccountSocial;
import com.example.weup.entity.Member;
import com.example.weup.entity.User;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.UserRepository;
import com.example.weup.security.JwtDto;
import com.example.weup.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final MemberRepository memberRepository;

    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    private final MailService mailService;

    private final S3Service s3Service;

    @Value("${user.default-profile-image}")
    private String defaultProfileImage;

    @Transactional
    public void signUp(SignUpRequestDto signUpRequestDto) {

        String email = signUpRequestDto.getEmail();
        if (!mailService.isEmailVerified(email)) {
            throw new GeneralException(ErrorInfo.EMAIL_NOT_VERIFIED);
        }

        User signUpUser = User.builder()
                .name(signUpRequestDto.getName())
                .role("ROLE_USER")
                .profileImage(defaultProfileImage)
                .build();

        AccountSocial accountSocial = AccountSocial.builder()
                .email(signUpRequestDto.getEmail())
                .password(passwordEncoder.encode(signUpRequestDto.getPassword()))
                .user(signUpUser)
                .build();

        signUpUser.setAccountSocial(accountSocial);

        userRepository.save(signUpUser);
    }

    @Transactional
    public GetProfileResponseDTO getProfile(String token) {
        Long userId = jwtUtil.getUserId(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        return GetProfileResponseDTO.builder()
                .name(user.getName())
                .email(user.getAccountSocial().getEmail())
                .profileImage(s3Service.getPresignedUrl(user.getProfileImage()))
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public ResponseEntity<DataResponseDTO<JwtDto>> reissueToken(String refreshToken) {
        if (refreshToken == null) {
            throw new GeneralException(ErrorInfo.REFRESH_TOKEN_NOT_FOUND);
        }

        if (jwtUtil.isExpired(refreshToken)) {
            throw new GeneralException(ErrorInfo.TOKEN_EXPIRED);
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        String newAccessToken = jwtUtil.createAccessToken(userId, user.getRole());
        String newRefreshToken = jwtUtil.createRefreshToken(userId);

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", newRefreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        JwtDto jwtDto = JwtDto.builder()
                .accessToken(newAccessToken)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(DataResponseDTO.of(jwtDto, "토큰 재발급 완료"));
    }

    @Transactional
    public void changePassword(String token, PasswordRequestDTO passwordRequestDTO) {

        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        if (!passwordEncoder.matches(passwordRequestDTO.getCurrentPassword(), user.getAccountSocial().getPassword())) {
            throw new GeneralException(ErrorInfo.UNAUTHORIZED);
        }

        user.getAccountSocial().setPassword(passwordEncoder.encode(passwordRequestDTO.getNewPassword()));
    }

    @Transactional
    public void editProfile(String token, String name, String phoneNumber, MultipartFile file) throws IOException {
        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        if (name != null) {
            user.setName(name.trim());
        }

        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber.trim());
        }

        if (file != null && !file.isEmpty()) {
            String existingImage = user.getProfileImage();
            if (existingImage != null && !existingImage.isEmpty()) {
                s3Service.deleteFile(existingImage);
            }

            String storedFileName = s3Service.uploadSingleFile(file).getStoredFileName();
            user.setProfileImage(storedFileName);
        }
    }

    @Transactional
    public void withdrawUser(String token) {
        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        user.setUserWithdrawal(true);

        List<Member> memberList = memberRepository.findAllByUser_UserId(userId);
        for (Member member : memberList) {
            member.setMemberDeleted(true);
        }
    }
}