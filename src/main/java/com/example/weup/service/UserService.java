package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.*;
import com.example.weup.dto.response.GetProfileResponseDTO;
import com.example.weup.entity.*;
import com.example.weup.repository.ChatMessageRepository;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.UserRepository;
import com.example.weup.security.JwtDto;
import com.example.weup.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final MemberRepository memberRepository;

    private final ChatMessageRepository chatMessageRepository;

    private final JwtUtil jwtUtil;

    private final StringRedisTemplate redisTemplate;

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

        signUpUser.linkAccount(accountSocial);

        userRepository.save(signUpUser);
    }

    @Transactional
    public GetProfileResponseDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        return GetProfileResponseDTO.builder()
                .name(user.getName())
                .email(user.getAccountSocial().getEmail())
                .profileImage(s3Service.getPresignedUrl(user.getProfileImage()))
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public JwtDto reissueToken(String refreshToken) {
        if (refreshToken == null) {
            throw new GeneralException(ErrorInfo.REFRESH_TOKEN_NOT_FOUND);
        }

        if (jwtUtil.isExpired(refreshToken)) {
            throw new GeneralException(ErrorInfo.TOKEN_EXPIRED);
        }

        Long userId = jwtUtil.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        String storedToken = redisTemplate.opsForValue().get("refreshToken:" + userId);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new GeneralException(ErrorInfo.INVALID_TOKEN);
        }

        String newAccessToken = jwtUtil.createAccessToken(userId, user.getRole());
        String newRefreshToken = jwtUtil.createRefreshToken(userId);

        return JwtDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(userId)
                .build();
    }

    @Transactional
    public void changePassword(Long userId, PasswordRequestDTO passwordRequestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        if (!passwordEncoder.matches(passwordRequestDTO.getCurrentPassword(), user.getAccountSocial().getPassword())) {
            throw new GeneralException(ErrorInfo.UNAUTHORIZED);
        }

        user.getAccountSocial().changePassword(passwordEncoder.encode(passwordRequestDTO.getNewPassword()));
    }

    @Transactional
    public void editProfile(Long userId, ProfileEditRequestDTO profileEditRequestDTO) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        user.editName(profileEditRequestDTO.getName())
            .editPhoneNumber(profileEditRequestDTO.getPhoneNumber());

        if (profileEditRequestDTO.getProfileImage() != null && !profileEditRequestDTO.getProfileImage().isEmpty()) {
            String existingImage = user.getProfileImage();
            if (existingImage != null && !existingImage.isEmpty()) {
                s3Service.deleteFile(existingImage);
            }

            String storedFileName = s3Service.uploadSingleFile(profileEditRequestDTO.getProfileImage()).getStoredFileName();
            user.updateProfileImage(storedFileName);
        }
    }

    @Transactional
    public void withdrawUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        user.withdraw();

        List<Member> leaders = memberRepository.findByUser_UserIdAndIsLeaderTrue(userId);

        for (Member leader : leaders) {
            Project project = leader.getProject();

            Optional<Member> nextLeaderOpt = memberRepository
                    .findAllByProjectAndUser_UserIdNotOrderByMemberIdAsc(project, userId).stream()
                    .filter(m -> !m.isMemberDeleted())
                    .findFirst();

            if (nextLeaderOpt.isPresent()) {
                Member nextLeader = nextLeaderOpt.get();
                nextLeader.promoteToLeader();
                leader.demoteFromLeader();
            } else {
                // todo. 프로젝트 삭제 로직 추가
            }
        }

        List<Member> memberList = memberRepository.findAllByUser_UserId(userId);
        for (Member member : memberList) {
            member.markAsDeleted();
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<User> expiredUsers = userRepository.findAllByDeletedAtBefore(threshold);

        User deletedUser = userRepository.findById(3L)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        for (User user : expiredUsers) {
            List<Member> members = memberRepository.findByUser(user);
            for (Member member : members) {
                member.assignDeletedUser(deletedUser);
            }
            memberRepository.saveAll(members);

            List<ChatMessage> messages = chatMessageRepository.findByUser(user);
//            for (ChatMessage message : messages) {
//                message.changeSender(deletedUser);
//            }
            chatMessageRepository.saveAll(messages);

            AccountSocial accountSocial = user.getAccountSocial();
            if (accountSocial != null) {
                String email = accountSocial.getEmail();
                if (email != null && !email.startsWith("deleted_")) {
                    accountSocial.markAsDeleted();
                }
            }
        }
    }


    @Transactional
    public void restoreWithdrawnUser(RestoreUserRequestDTO restoreUserRequestDTO) {
        User user = userRepository.findById(restoreUserRequestDTO.getUserId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        if (user.getDeletedAt() == null) {
            throw new GeneralException(ErrorInfo.USER_IS_NOT_WITHDRAWN);
        }

        user.restore();
    }

    public void logout(Long userId, String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GeneralException(ErrorInfo.REFRESH_TOKEN_NOT_FOUND);
        }

        redisTemplate.delete("refreshToken:" + userId);
    }
}
