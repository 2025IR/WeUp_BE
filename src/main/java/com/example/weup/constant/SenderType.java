package com.example.weup.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SenderType {
    MEMBER("", ""),
    AI("AI 비서", "64a81f23-0738-433b-86d8-44b3cd3bc553-smiley2.png"),
    SYSTEM("시스템", ""),
    WITHDRAW("알 수 없음", "delete_user.jpg");

    private final String name;
    private final String profileImage;
}

