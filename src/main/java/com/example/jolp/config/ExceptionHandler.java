package com.example.jolp.config;

import com.example.jolp.GeneralException;
import com.example.jolp.constant.Code;
import com.example.jolp.dto.ErrorResponseDTO;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionHandler extends ResponseEntityExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler
    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
        return handleExceptionInternal(e, Code.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);
    }

//    @org.springframework.web.bind.annotation.ExceptionHandler
//    public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException e, WebRequest request) {
//        return handleExceptionInternal(e, Code.VALIDATION_ERROR, request);
//    }

    @org.springframework.web.bind.annotation.ExceptionHandler
    public ResponseEntity<Object> general(GeneralException e, WebRequest request) {
        return handleExceptionInternal(e, e.getErrorCode(), e.getErrorCode().getHttpStatus());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        return handleExceptionInternal(e, Code.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
        return handleExceptionInternal(e, Code.USER_ALREADY_EXIST, HttpStatus.CONFLICT);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Object> handleUsernameNotFoundException(UsernameNotFoundException e, WebRequest request) {
        return handleExceptionInternal(e, Code.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException e, WebRequest request) {
        return handleExceptionInternal(e, Code.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<Object> handleAuthenticationServiceException(AuthenticationServiceException e, WebRequest request) {
        return handleExceptionInternal(e, Code.BAD_REQUEST, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of(e));
    }
    // 기본으로 해결해주는 오류라 중복 문제가 생길 경우
    // 이렇게 상속받은 걸 이용해서 오버라이딩해서 오류 지정해주면 됨!
    // 드디어찾았다ㅋㅋ







//    @Override
//    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
//                                                             HttpHeaders headers, HttpStatus status, WebRequest request) {
//        return handleExceptionInternal(ex, Code.valueOf(status), headers, status, request);
//    }


    private ResponseEntity<Object> handleExceptionInternal(Exception e, Code errorCode, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(ErrorResponseDTO.of(errorCode, e));
    }


}