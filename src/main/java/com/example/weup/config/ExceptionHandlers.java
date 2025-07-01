package com.example.weup.config;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.ErrorResponseDTO;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionHandlers {

    @ExceptionHandler
    public ResponseEntity<Object> general(GeneralException e) {
        return handleExceptionInternal(e, e.getErrorInfo(), e.getErrorInfo().getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = (fieldError != null) ? fieldError.getDefaultMessage() : "잘못된 요청입니다.";
        return handleExceptionInternal(e, ErrorInfo.EMPTY_INPUT_VALUE, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraint(ConstraintViolationException e) {
        String message = e.getMessage().split(":").length > 1
                ? e.getMessage().split(":")[1].trim()
                : "잘못된 요청입니다.";
        return handleExceptionInternal(e, ErrorInfo.EMPTY_INPUT_VALUE, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e) {
        return handleExceptionInternal(e, ErrorInfo.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorInfo errorInfo, HttpStatus status) {
        return handleExceptionInternal(e, errorInfo, status, errorInfo.getMessage());
    }

    private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorInfo errorInfo, HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(ErrorResponseDTO.of(errorInfo, message));
    }
}
