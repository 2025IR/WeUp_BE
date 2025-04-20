package com.example.weup;

import com.example.weup.constant.ErrorInfo;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

    private final ErrorInfo errorInfo;

    public GeneralException(ErrorInfo errorInfo) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
    }
}