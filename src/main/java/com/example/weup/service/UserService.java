package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.SignUpRequestDto;
import com.example.weup.dto.request.TokenRequestDTO;
import com.example.weup.dto.request.PasswordRequestDTO;
import com.example.weup.dto.response.GetProfileResponseDTO;
import com.example.weup.entity.AccountSocial;
import com.example.weup.entity.User;
import com.example.weup.repository.UserRepository;
import com.example.weup.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    private final MailService mailService;

    private final S3Service s3Service;

    @Transactional
    public void signUp(SignUpRequestDto signUpRequestDto) {

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
                .profileImage(s3Service.getPresignedUrl(user.getProfileImage()))
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
    public Map<String, String> reissue(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        String newAccessToken = jwtUtil.createAccessToken(userId, user.getRole());

        return Map.of("access_token", newAccessToken);
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
    }
}