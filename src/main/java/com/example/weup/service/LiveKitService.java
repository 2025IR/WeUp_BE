package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.Project;
import com.example.weup.entity.User;
import com.example.weup.repository.UserRepository;
import com.example.weup.validate.MemberValidator;
import com.example.weup.validate.ProjectValidator;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.livekit.server.AccessToken;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitService {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final MemberValidator memberValidator;
    private final ProjectValidator projectValidator;

    @Value("${vite.api-key}")
    private String apiKey;

    @Value("${vite.api-secret}")
    private String apiSecret;

    public String generateLiveKitToken(Long projectId, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        Project project = projectValidator.validateActiveProject(projectId);
        memberValidator.validateActiveMemberInProject(userId, projectId);

        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(user.getName());
        token.setIdentity(userId.toString());
        token.setMetadata(user.getProfileImage());
        token.setTtl(3600);
        token.addGrants(new RoomJoin(true), new RoomName(project.getRoomName()));

        log.info("enter meeting room -> generate livekit token : user id - {}, room name - {}", userId, project.getRoomName());
        return token.toJwt();
    }

    @Transactional
    public void enterRoom(Long projectId, Long userId) {

        memberValidator.validateActiveMemberInProject(userId, projectId);

        String key = "meeting:" + projectId + ":users";
        log.info("enter meeting room -> enter : user id - {}, room name - {}", userId, projectId);
        redisTemplate.opsForSet().add(key, userId.toString());
    }

    @Transactional
    public void leaveRoom(Long projectId, Long userId) {

        projectValidator.validateActiveProject(projectId);
        memberValidator.validateActiveMemberInProject(userId, projectId);

        String key = "meeting:" + projectId + ":users";
        log.info("leave meeting room -> enter : user id - {}, room name - {}", userId, projectId);
        redisTemplate.opsForSet().remove(key, userId.toString());
    }

    public Long getParticipantCount(Long projectId, Long userId) {

        projectValidator.validateActiveProject(projectId);
        memberValidator.validateActiveMemberInProject(userId, projectId);

        String key = "meeting:" + projectId + ":users";
        log.info("get participant count -> db read success : room name - {}", projectId);
        return redisTemplate.opsForSet().size(key);
    }
}
