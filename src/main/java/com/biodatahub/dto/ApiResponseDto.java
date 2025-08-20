package com.biodatahub.dto;

import lombok.*;

/**
 * 표준 API 응답 DTO
 */
@Value
@Builder
public class ApiResponseDto<T> {
    boolean success;
    T data;
    String message;
    
    public static <T> ApiResponseDto<T> success(T data) {
        return ApiResponseDto.<T>builder()
            .success(true)
            .data(data)
            .build();
    }
    
    public static <T> ApiResponseDto<T> error(String message) {
        return ApiResponseDto.<T>builder()
            .success(false)
            .message(message)
            .build();
    }
}