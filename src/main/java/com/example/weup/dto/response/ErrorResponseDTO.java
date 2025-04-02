package com.example.weup.dto.response;

import com.example.weup.constant.ErrorInfo;
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

    private ErrorResponseDTO(ErrorInfo errorInfo, Map<String, Object> details) {
        this.error = errorInfo.name();
        this.message = errorInfo.getMessage();
        this.timestamp = LocalDateTime.now().toString();
        this.status = errorInfo.getHttpStatus().value();
        this.details = details;
    }

    private ErrorResponseDTO(ErrorInfo errorInfo, String message) {
        this.error = errorInfo.name();
        this.message = message;
        this.timestamp = LocalDateTime.now().toString();
        this.status = errorInfo.getHttpStatus().value();
        this.details = null;
    }

    private ErrorResponseDTO(ErrorInfo errorInfo, String message, Map<String, Object> details) {
        this.error = errorInfo.name();
        this.message = message;
        this.timestamp = LocalDateTime.now().toString();
        this.status = errorInfo.getHttpStatus().value();
        this.details = details;
    }

    public static ErrorResponseDTO of(ErrorInfo errorInfo) {
        return new ErrorResponseDTO(errorInfo, errorInfo.getMessage());
    }

    public static ErrorResponseDTO of(ErrorInfo errorInfo, Exception e) {
        return new ErrorResponseDTO(errorInfo, errorInfo.getMessage(e));
    }

    public static ErrorResponseDTO of(ErrorInfo errorInfo, String message) {
        return new ErrorResponseDTO(errorInfo, message);
    }

    public static ErrorResponseDTO of(MethodArgumentNotValidException e) {
        Map<String, Object> details = new HashMap<>();
        
        BindingResult bindingResult = e.getBindingResult();
        bindingResult.getFieldErrors().forEach(error -> {
            details.put(error.getField(), error.getDefaultMessage());
        });
        
        return new ErrorResponseDTO(ErrorInfo.BAD_REQUEST, "입력값 검증 오류가 발생했습니다.", details);
    }
} 