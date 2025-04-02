package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.config.JwtProperties;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.JoinDTO;
import com.example.weup.dto.response.TokenResponseDTO;
import com.example.weup.entity.UserEntity;
import com.example.weup.jwt.JWTUtil;
import com.example.weup.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JWTUtil jwtUtil;
    private final JwtProperties jwtProperties;


    public void joinProcess(JoinDTO joinDTO) {
        String name = joinDTO.getName();
        String email = joinDTO.getEmail();
        String password = joinDTO.getPassword();
        LocalDateTime PWExpirationDate = LocalDate.now().plusDays(90).atStartOfDay();

        // email 중복 확인
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + email);
        }

        if (name == null || email == null || password == null) {
            throw new AuthenticationServiceException("모든 필드는 필수입니다");
        }

        // 새 사용자 저장
        UserEntity data = new UserEntity();
        data.setName(name);
        data.setEmail(email);
        data.setPassword(bCryptPasswordEncoder.encode(password));
        data.setRole("ROLE_USER");
        data.setProfileImage("base_image");
        data.setPasswordExpirationDate(PWExpirationDate);
        userRepository.save(data);
    }

    @Transactional
    public TokenResponseDTO refreshToken(String refreshToken) {
        if (!refreshToken.startsWith("Bearer ")) {
            throw new GeneralException(ErrorInfo.VALIDATION_ERROR, "토큰은 Bearer 형식이어야 합니다");
        }

        // Bearer 제거
        String tokenWithoutPrefix = refreshToken.substring(7);
        
        try {
            // 1. 리프레시 토큰 검증
            jwtUtil.validateToken(tokenWithoutPrefix);
            
            // 2. 토큰이 리프레시 토큰 타입인지 확인
            String tokenType = jwtUtil.getType(tokenWithoutPrefix);
            if (!"refresh".equals(tokenType)) {
                throw new GeneralException(ErrorInfo.VALIDATION_ERROR, "유효한 리프레시 토큰이 아닙니다");
            }
            
            // 3. 토큰에서 사용자 ID 추출
            Long userId = jwtUtil.getUserId(tokenWithoutPrefix);
            
            // 4. 리프레시 토큰으로 사용자 조회
            UserEntity user = userRepository.findByRefreshToken(refreshToken)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.UNAUTHORIZED, "저장된 리프레시 토큰을 찾을 수 없습니다"));
            
            // 사용자 ID가 토큰의 ID와 일치하는지 확인
            if (!user.getId().equals(userId)) {
                throw new GeneralException(ErrorInfo.UNAUTHORIZED, "토큰의 사용자 정보가 일치하지 않습니다");
            }
            
            // 5. 새로운 토큰 발급
            String role = user.getRole();
            String newAccessToken = jwtUtil.createAccessToken(
                    userId,
                    role,
                    jwtProperties.getAccessToken().getExpiration()
            );
            
            String newRefreshToken = jwtUtil.createRefreshToken(
                    userId,
                    jwtProperties.getRefreshToken().getExpiration()
            );
            
            // 6. 새로운 리프레시 토큰을 DB에 저장
            user.setRefreshToken("Bearer " + newRefreshToken);
            userRepository.save(user);
            
            // 7. 토큰 반환
            return new TokenResponseDTO("Bearer " + newAccessToken, "Bearer " + newRefreshToken);
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR, "토큰 갱신 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
