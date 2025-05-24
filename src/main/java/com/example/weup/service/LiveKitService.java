package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.Project;
import com.example.weup.entity.User;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.ProjectRepository;
import com.example.weup.repository.UserRepository;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.livekit.server.AccessToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitService {

    private final RedisTemplate<String, String> redisTemplate;

    private final ProjectRepository projectRepository;

    private final UserRepository userRepository;

    private final MemberRepository memberRepository;

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    @Transactional
    public String generateLiveKitToken(Long projectId, Long userId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        if (!memberRepository.existsByUser_UserIdAndProject_ProjectId(userId, projectId)) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        if (project.getRoomName() == null) {
            String roomName = String.valueOf(project.getProjectId());
            project.setRoomName(roomName);

            projectRepository.save(project);
        }

        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(user.getName());
        token.setIdentity(userId.toString());
        token.setMetadata(user.getProfileImage());
        token.setTtl(3600);
        token.addGrants(new RoomJoin(true), new RoomName(project.getProjectId().toString()));

        return token.toJwt();
    }

    @Transactional
    public void enterRoom(Long projectId, Long userId) {

        if (!memberRepository.existsByUser_UserIdAndProject_ProjectId(userId, projectId)) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        String key = "meeting:" + projectId + ":users";
        redisTemplate.opsForSet().add(key, userId.toString());
    }

    @Transactional
    public void leaveRoom(Long projectId, Long userId) {

        if (!memberRepository.existsByUser_UserIdAndProject_ProjectId(userId, projectId)) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        String key = "meeting:" + projectId + ":users";
        redisTemplate.opsForSet().remove(key, userId.toString());
    }

    @Transactional
    public Long getRoomUserCount(Long projectId, Long userId) {

        if (!memberRepository.existsByUser_UserIdAndProject_ProjectId(userId, projectId)) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        String key = "meeting:" + projectId + ":users";
        return redisTemplate.opsForSet().size(key);
    }
}
