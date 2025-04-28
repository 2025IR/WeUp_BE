package com.example.weup.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum ErrorInfo {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러가 발생했습니다."),

    EMAIL_ALREADY_EXIST(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증에 실패하였습니다."),

    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),

    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    
    EMAIL_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "이메일 인증에 실패했습니다."),
    
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public String getMessage() {
        return String.format(this.message);
    }
}