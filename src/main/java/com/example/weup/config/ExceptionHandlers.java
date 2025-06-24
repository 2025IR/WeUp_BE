package com.example.weup.config;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionHandlers extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<Object> general(GeneralException e) {
        return handleExceptionInternal(e, e.getErrorInfo(), e.getErrorInfo().getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e) {
        return handleExceptionInternal(e, ErrorInfo.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorInfo errorInfo, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(ErrorResponseDTO.of(errorInfo, errorInfo.getMessage()));
        //TODO. Exception e를 가져오는거면 errorInfo.getMessage()가 아니라 e를 반환해도 되지 않을까?
        //TODO. 아니면 Exception e를 반환하지 않는게?
    }
}