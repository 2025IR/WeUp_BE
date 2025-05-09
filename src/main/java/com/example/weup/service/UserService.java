package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.controller.MailController;
import com.example.weup.dto.request.ProfileEditRequestDTO;
import com.example.weup.dto.request.SignUpRequestDto;
import com.example.weup.dto.request.TokenRequestDTO;
import com.example.weup.dto.request.PasswordRequestDTO;
import com.example.weup.dto.response.GetProfileResponseDTO;
import com.example.weup.entity.AccountSocial;
import com.example.weup.entity.User;
import com.example.weup.repository.UserRepository;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.MailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    private final MailService mailService;

    @Transactional
    public void signUp(SignUpRequestDto signUpRequestDto) {

        if (userRepository.existsByAccountSocialEmail(signUpRequestDto.getEmail())) {
            throw new GeneralException(ErrorInfo.EMAIL_ALREADY_EXIST);
        }
        
        // 이메일 인증 상태 확인
        String email = signUpRequestDto.getEmail();
        if (!mailService.isEmailVerified(email)) {
            throw new GeneralException(ErrorInfo.EMAIL_NOT_VERIFIED);
        }

        User signUpUser = User.builder()
                .name(signUpRequestDto.getName())
                .role("ROLE_USER")
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
                .profileImage(user.getProfileImage())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    @Transactional
    public Map<String, String> reissuetoken(TokenRequestDTO tokenRequestDTO){
        String refreshToken = tokenRequestDTO.getRefreshToken();

        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        String newAccessToken = jwtUtil.createAccessToken(userId, user.getRole());
        String newRefreshToken = jwtUtil.createRefreshToken(userId);

        return Map.of("access_token", newAccessToken, "refresh_token", newRefreshToken);
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
    public void editProfile(String token, ProfileEditRequestDTO profileEditRequestDTO) {
        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        if (profileEditRequestDTO.getName() != null && !profileEditRequestDTO.getName().isEmpty()) {
            user.setName(profileEditRequestDTO.getName());
        }

        if (profileEditRequestDTO.getProfileImage() != null && !profileEditRequestDTO.getProfileImage().isEmpty()) {
            user.setProfileImage(profileEditRequestDTO.getProfileImage());
        }
        
        if (profileEditRequestDTO.getPhoneNumber() != null && !profileEditRequestDTO.getPhoneNumber().isEmpty()) {
            user.setPhoneNumber(profileEditRequestDTO.getPhoneNumber());
        }
    }

    @Transactional
    public void withdrawUser(String token) {
        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        user.setUserWithdrawal(true);
    }
}