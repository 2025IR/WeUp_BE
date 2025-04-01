package com.example.jolp.dto;

import com.example.jolp.constant.Code;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {
    private final Map<String, Object> details;
    private final String error;
    private final String message;
    private final String timestamp;
    private final int status;
    private final int code;

    private ErrorResponseDTO(Code errorCode, Map<String, Object> details) {
        this.error = errorCode.name();
        this.message = errorCode.getMessage();
        this.timestamp = LocalDateTime.now().toString();
        this.status = errorCode.getHttpStatus().value();
        this.code = errorCode.getCode();
        this.details = details;
    }

    private ErrorResponseDTO(Code errorCode, String message) {
        this.error = errorCode.name();
        this.message = message;
        this.timestamp = LocalDateTime.now().toString();
        this.status = errorCode.getHttpStatus().value();
        this.code = errorCode.getCode();
        this.details = null;
    }

    private ErrorResponseDTO(Code errorCode, String message, Map<String, Object> details) {
        this.error = errorCode.name();
        this.message = message;
        this.timestamp = LocalDateTime.now().toString();
        this.status = errorCode.getHttpStatus().value();
        this.code = errorCode.getCode();
        this.details = details;
    }

    public static ErrorResponseDTO of(Code errorCode) {
        return new ErrorResponseDTO(errorCode, errorCode.getMessage());
    }

    public static ErrorResponseDTO of(Code errorCode, Exception e) {
        return new ErrorResponseDTO(errorCode, errorCode.getMessage(e));
    }

    public static ErrorResponseDTO of(Code errorCode, String message) {
        return new ErrorResponseDTO(errorCode, message);
    }

    public static ErrorResponseDTO of(MethodArgumentNotValidException e) {
        Map<String, Object> details = new HashMap<>();
        
        BindingResult bindingResult = e.getBindingResult();
        bindingResult.getFieldErrors().forEach(error -> {
            details.put(error.getField(), error.getDefaultMessage());
        });
        
        return new ErrorResponseDTO(Code.BAD_REQUEST, "입력값 검증 오류가 발생했습니다.", details);
    }
}