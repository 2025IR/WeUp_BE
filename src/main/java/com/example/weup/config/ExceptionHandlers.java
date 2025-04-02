package com.example.weup.config;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.ErrorResponseDTO;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionHandlers extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
        return handleExceptionInternal(e, ErrorInfo.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<Object> general(GeneralException e, WebRequest request) {
        return handleExceptionInternal(e, e.getErrorInfo(), e.getErrorInfo().getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        return handleExceptionInternal(e, ErrorInfo.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
        return handleExceptionInternal(e, ErrorInfo.USER_ALREADY_EXIST, HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleUsernameNotFoundException(UsernameNotFoundException e, WebRequest request) {
        return handleExceptionInternal(e, ErrorInfo.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException e, WebRequest request) {
        return handleExceptionInternal(e, ErrorInfo.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleAuthenticationServiceException(AuthenticationServiceException e, WebRequest request) {
        return handleExceptionInternal(e, ErrorInfo.BAD_REQUEST, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of(e));
    }
    // 기본으로 해결해주는 오류라 중복 문제가 생길 경우 이렇게 상속받은 걸 이용해서 오버라이딩해서 오류 지정 가능

    private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorInfo errorInfo, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(ErrorResponseDTO.of(errorInfo, e));
    }
}