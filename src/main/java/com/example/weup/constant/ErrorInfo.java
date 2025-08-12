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

    ALREADY_IN_PROJECT(HttpStatus.CONFLICT, "이미 프로젝트에 속해 있는 유저입니다."),

    ROLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 프로젝트에 이미 존재하는 역할입니다."),

    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    
    EMAIL_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "이메일 인증에 실패했습니다."),
    
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다."),

    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글 태그입니다."),
    
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
  
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "역할을 찾을 수 없습니다."),

    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다."),

    INVALID_TOKEN(HttpStatus.FORBIDDEN, "토큰이 유효하지 않습니다."),

    TOKEN_EXPIRED(HttpStatus.FORBIDDEN, "토큰이 만료되었습니다."),

    NOT_IN_PROJECT(HttpStatus.FORBIDDEN, "해당 프로젝트에 접근 권한이 없습니다."),

    DELETED_MEMBER(HttpStatus.FORBIDDEN, "해당 프로젝트에서 탈퇴된 멤버입니다."),

    NOT_WRITER(HttpStatus.FORBIDDEN, "해당 게시글의 작성자가 아닙니다."),

    EMPTY_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력란이 입력되지 않았습니다."),

    NOT_LEADER(HttpStatus.FORBIDDEN, "프로젝트 리더 권한이 필요합니다."),

    ENDED_PROJECT(HttpStatus.FORBIDDEN, "종료된 프로젝트입니다."),

    DELETED_PROJECT(HttpStatus.FORBIDDEN, "삭제된 프로젝트입니다."),

    USER_IS_NOT_WITHDRAWN(HttpStatus.BAD_REQUEST, "탈퇴한 유저가 아닙니다."),

    PROJECT_IS_NOT_DELETED(HttpStatus.BAD_REQUEST, "삭제되지 않은 프로젝트입니다."),

    IS_EDITING_NOW(HttpStatus.CONFLICT, "다른 사용자가 수정 중입니다."),

    MEMBER_ALREADY_EXISTS_IN_CHAT_ROOM(HttpStatus.CONFLICT, "이미 채팅방에 존재하는 멤버입니다.");

    private final HttpStatus httpStatus;

    private final String message;

    public String getMessage() {
        return String.format(this.message);
    }
}