package com.example.weup.constant;

import com.example.weup.GeneralException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public enum ErrorInfo {

    OK(HttpStatus.OK, "Ok"),

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation error"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Requested resource is not found"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error"),
    DATA_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Data access error"),

    USER_PASSWORD_EXPIRED(HttpStatus.UNAUTHORIZED, "User password expired"),
    USER_ALREADY_EXIST(HttpStatus.CONFLICT, "This username is already in use"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),

    WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "Wrong password"),

    // 401 오류, 인증되지 않은 사용자 오류
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "User unauthorized"),

    // 403 오류여야 하기 때문에 BAD_REQUEST -> FORBIDDEN 으로 수정, 인증은 됐는데 권한이 없는 사용자 오류
    FORBIDDEN(HttpStatus.FORBIDDEN, "access denied"),

    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid username or password");

    private final HttpStatus httpStatus;
    private final String message;

    public String getMessage(Throwable e) {
        return String.format("%s - %s", this.message, Optional.ofNullable(e.getMessage()).orElse(""));
    }

    public String getMessage(String message) {
        return Optional.ofNullable(message)
                .filter(Predicate.not(String::isBlank))
                .orElse(this.getMessage());
    }

    public static ErrorInfo valueOf(HttpStatus httpStatus) {
        if (httpStatus == null) {
            throw new GeneralException("HttpStatus is null.");
        }

        return Arrays.stream(values())
                .filter(errorInfo -> errorInfo.getHttpStatus() == httpStatus)
                .findFirst()
                .orElseGet(() -> {
                    if (httpStatus.is4xxClientError()) {
                        return ErrorInfo.BAD_REQUEST;
                    } else if (httpStatus.is5xxServerError()) {
                        return ErrorInfo.INTERNAL_ERROR;
                    } else {
                        return ErrorInfo.OK;
                    }
                });
    }

    @Override
    public String toString() {
        return String.format("%s", this.name());
    }
}