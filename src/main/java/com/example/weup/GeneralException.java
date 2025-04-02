package com.example.weup;

import com.example.weup.constant.ErrorInfo;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

    private final ErrorInfo errorInfo;

    public GeneralException() {
        super(ErrorInfo.INTERNAL_ERROR.getMessage());
        this.errorInfo = ErrorInfo.INTERNAL_ERROR;
    }

    public GeneralException(String message) {
        super(ErrorInfo.INTERNAL_ERROR.getMessage(message));
        this.errorInfo = ErrorInfo.INTERNAL_ERROR;
    }

    public GeneralException(String message, Throwable cause) {
        super(ErrorInfo.INTERNAL_ERROR.getMessage(message), cause);
        this.errorInfo = ErrorInfo.INTERNAL_ERROR;
    }

    public GeneralException(Throwable cause) {
        super(ErrorInfo.INTERNAL_ERROR.getMessage(cause));
        this.errorInfo = ErrorInfo.INTERNAL_ERROR;
    }

    public GeneralException(ErrorInfo errorInfo) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
    }

    public GeneralException(ErrorInfo errorInfo, String message) {
        super(errorInfo.getMessage(message));
        this.errorInfo = errorInfo;
    }

    public GeneralException(ErrorInfo errorInfo, String message, Throwable cause) {
        super(errorInfo.getMessage(message), cause);
        this.errorInfo = errorInfo;
    }

    public GeneralException(ErrorInfo errorInfo, Throwable cause) {
        super(errorInfo.getMessage(cause), cause);
        this.errorInfo = errorInfo;
    }
}