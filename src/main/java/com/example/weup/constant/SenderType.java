package com.example.weup.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SenderType {
    MEMBER("", ""),
    AI("AI 비서", "55620a2f-3742-43aa-a6dc-945092bcdbe9-AI_1.png"),
    SYSTEM("시스템", ""),
    WITHDRAW("알 수 없음", "delete_user.jpg");

    private final String name;
    private final String profileImage;
}

