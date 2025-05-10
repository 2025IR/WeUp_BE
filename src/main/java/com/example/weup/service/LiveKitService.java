package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.Project;
import com.example.weup.entity.User;
import com.example.weup.repository.ProjectRepository;
import com.example.weup.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LiveKitService {

    private final RedisTemplate<String, String> redisTemplate;

    private final ProjectRepository projectRepository;

    private final UserRepository userRepository;

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    @Value("${livekit.host}")
    private String liveKitHost;

    @Transactional
    public String generateLiveKitToken(Long projectId, Long userId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        if (project.getRoomName() == null) {
            String roomName = project.getProjectName() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            project.setRoomName(roomName);
            projectRepository.save(project);
        }

        long expMillis = System.currentTimeMillis() + 3600_000;

        Map<String, Object> livekitClaims = new HashMap<>();
        livekitClaims.put("room", project.getRoomName());
        livekitClaims.put("userId", user.getUserId());
        livekitClaims.put("profileImage", user.getProfileImage());

        return Jwts.builder()
                .setSubject(user.getUserId().toString())
                .setIssuer(apiKey)
                .addClaims(livekitClaims)
                .setExpiration(new Date(expMillis))
                .signWith(SignatureAlgorithm.HS256, apiSecret)
                .compact();
    }

    @Transactional
    public void enterRoom(Long projectId, Long userId) {
        String key = "project:" + projectId + ":users";
        redisTemplate.opsForSet().add(key, userId.toString());
    }

    @Transactional
    public void leaveRoom(Long projectId, Long userId) {
        String key = "project:" + projectId + ":users";
        redisTemplate.opsForSet().remove(key, userId.toString());

        if (getRoomUserCount(projectId) == 0) {

        }
    }

    @Transactional
    public Long getRoomUserCount(Long projectId) {
        String key = "project:" + projectId + ":users";
        return redisTemplate.opsForSet().size(key);
    }
}
