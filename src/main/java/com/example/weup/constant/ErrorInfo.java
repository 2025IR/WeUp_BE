package com.example.weup.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum ErrorInfo {

    OK(HttpStatus.OK, "Ok"),

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error"),

    USER_ALREADY_EXIST(HttpStatus.CONFLICT, "This username is already in use"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "User unauthorized"),

    INVALID_JWT(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token"),

    FORBIDDEN(HttpStatus.FORBIDDEN, "access denied");

    private final HttpStatus httpStatus;
    private final String message;

    public String getMessage() {
        return String.format(this.message);
    }
}