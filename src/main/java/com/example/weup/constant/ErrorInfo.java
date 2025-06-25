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

    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 프로젝트를 찾을 수 없습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "일치하는 멤버가 없습니다."),
  
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 투두를 찾을 수 없습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증에 실패하였습니다."),

    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 파일 업로드에 실패했습니다."),

    AlREADY_IN_PROJECT(HttpStatus.CONFLICT, "이미 프로젝트에 속해 있는 유저입니다."),

    ROLE_ALREADY_GIVEN(HttpStatus.CONFLICT, "해당 멤버에게 이미 부여된 역할입니다."),

    ROLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 프로젝트에 이미 존재하는 역할입니다."),

    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    
    EMAIL_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "이메일 인증에 실패했습니다."),
    
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다."),

    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글 태그입니다."),
    
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
  
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "역할을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public String getMessage() {
        return String.format(this.message);
    }
}