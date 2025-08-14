package com.example.weup.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    MEMBER_INVITED("%s님이 %s 프로젝트에 초대되었습니다."),
    MEMBER_DELETED("%s님이 %s 프로젝트에서 탈퇴되었습니다."),
    LEADER_DELEGATED("%s님의 권한이 팀장으로 변경되었습니다."),
    PROJECT_ENDED("%s 프로젝트가 종료되었습니다.");

    private final String message;

    public String format(Object... args) {
        return String.format(message, args);
    }
}
