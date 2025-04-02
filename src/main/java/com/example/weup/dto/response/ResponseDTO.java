package com.example.weup.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDTO {
    private final Boolean success;
    private final String message;

    public ResponseDTO(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ResponseDTO success() {
        return new ResponseDTO(true, "처리가 완료되었습니다.");
    }

    public static ResponseDTO success(String message) {
        return new ResponseDTO(true, message);
    }
} 