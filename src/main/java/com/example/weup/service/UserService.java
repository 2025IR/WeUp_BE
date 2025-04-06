package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.config.JwtProperties;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.SignInRequestDto;
import com.example.weup.dto.request.SignUpRequestDto;
import com.example.weup.dto.response.TokenResponseDTO;
import com.example.weup.entity.User;
import com.example.weup.jwt.JWTUtil;
import com.example.weup.jwt.JwtDto;
import com.example.weup.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @Transactional
    public void signUp(SignUpRequestDto signUpRequestDto) {

        // email 중복 확인
        if (userRepository.existsByEmail(signUpRequestDto.getEmail())) {
            //throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + signUpRequestDto.getEmail());  Custom Error Response
            throw new GeneralException(ErrorInfo.USER_ALREADY_EXIST);
        }

//      앞에서 @NotBlank, @Valid 값으로 예외처리를 해줬는데 또 해줄 필요는 없음
//        if (name == null || email == null || password == null) {
//            throw new AuthenticationServiceException("모든 필드는 필수입니다");
//        }

        LocalDateTime PWExpirationDate = LocalDate.now().plusDays(90).atStartOfDay();

        // 새 사용자 저장
        User signUpUser = User.builder()
                .name(signUpRequestDto.getName())
                .email(signUpRequestDto.getEmail())
                .password(bCryptPasswordEncoder.encode(signUpRequestDto.getPassword()))
                .profileImage("base_image")
                .role("ROLE_USER")
                .passwordExpirationDate(PWExpirationDate)
                .build();

        userRepository.save(signUpUser);
    }

    @Transactional
    public JwtDto signIn(SignInRequestDto signInRequestDto) {
        User user = userRepository.findByEmail(signInRequestDto.getEmail())
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        if (!bCryptPasswordEncoder.matches(signInRequestDto.getPassword(), user.getPassword())) {
            throw new GeneralException(ErrorInfo.WRONG_PASSWORD);
        }

        //TODO. JwtUtil.createAccessToken, createRefreshToken 메소드의 expiredMs 수정하기
        return JwtDto.builder()
                .accessToken(jwtUtil.createAccessToken(user.getUserId(), user.getRole(), 0))
                .refreshToken(jwtUtil.createRefreshToken(user.getUserId(), 0))
                .build();

        // refreshToken 저장
    }

//    @Transactional
//    public TokenResponseDTO refreshToken(String refreshToken) {
//        if (!refreshToken.startsWith("Bearer ")) {
//            throw new GeneralException(ErrorInfo.VALIDATION_ERROR, "토큰은 Bearer 형식이어야 합니다");
//        }
//
//        // Bearer 제거
//        String tokenWithoutPrefix = refreshToken.substring(7);
//
//        try {
//            // 1. 리프레시 토큰 검증
//            jwtUtil.validateToken(tokenWithoutPrefix);
//
//            // 2. 토큰이 리프레시 토큰 타입인지 확인
//            String tokenType = jwtUtil.getType(tokenWithoutPrefix);
//            if (!"refresh".equals(tokenType)) {
//                throw new GeneralException(ErrorInfo.VALIDATION_ERROR, "유효한 리프레시 토큰이 아닙니다");
//            }
//
//            // 3. 토큰에서 사용자 ID 추출
//            Long userId = jwtUtil.getUserId(tokenWithoutPrefix);
//
//            // 4. 리프레시 토큰으로 사용자 조회 -> ???????????????????????
//            User user = userRepository.findByRefreshToken(refreshToken)
//                    .orElseThrow(() -> new GeneralException(ErrorInfo.UNAUTHORIZED, "저장된 리프레시 토큰을 찾을 수 없습니다"));
//
//            // 사용자 ID가 토큰의 ID와 일치하는지 확인
//            if (!user.getId().equals(userId)) {
//                throw new GeneralException(ErrorInfo.UNAUTHORIZED, "토큰의 사용자 정보가 일치하지 않습니다");
//            }
//
//            // 5. 새로운 토큰 발급
//            String role = user.getRole();
//            String newAccessToken = jwtUtil.createAccessToken(
//                    userId,
//                    role,
//                    jwtProperties.getAccessToken().getExpiration()
//            );
//
//            String newRefreshToken = jwtUtil.createRefreshToken(
//                    userId,
//                    jwtProperties.getRefreshToken().getExpiration()
//            );
//
//            // 6. 새로운 리프레시 토큰을 DB에 저장
//            user.setRefreshToken("Bearer " + newRefreshToken);
//            userRepository.save(user);
//            
//            // 7. 토큰 반환
//            return new TokenResponseDTO("Bearer " + newAccessToken, "Bearer " + newRefreshToken);
//        } catch (GeneralException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new GeneralException(ErrorInfo.INTERNAL_ERROR, "토큰 갱신 중 오류가 발생했습니다: " + e.getMessage());
//        }
//    }
}
