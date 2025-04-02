package com.example.weup.dto.response;

import lombok.Getter;

@Getter
public class DataResponseDTO<T> extends ResponseDTO {

    private final T data;

    private DataResponseDTO(T data) {
        super(true, "처리가 완료되었습니다.");
        this.data = data;
    }

    private DataResponseDTO(T data, String message) {
        super(true, message);
        this.data = data;
    }

    public static <T> DataResponseDTO<T> of(T data) {
        return new DataResponseDTO<>(data);
    }

    public static <T> DataResponseDTO<T> of(T data, String message) {
        return new DataResponseDTO<>(data, message);
    }

    public static <T> DataResponseDTO<T> empty() {
        return new DataResponseDTO<>(null);
    }
} 