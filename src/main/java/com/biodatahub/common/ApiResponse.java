package com.biodatahub.common;

import lombok.*;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * API 응답을 위한 공통 컴포넌트
 */
@Value
public class ApiResponse {
    boolean success;
    Object data;
    String message;
    
    public static ApiResponse success(Object data) {
        return new ApiResponse(true, data, null);
    }
    
    public static ApiResponse success() {
        return new ApiResponse(true, null, null);
    }
    
    public static ApiResponse error(String message) {
        return new ApiResponse(false, null, message);
    }
    
    /**
     * ResponseEntity로 변환 (기존 코드와 호환성 유지)
     */
    public ResponseEntity<Map<String, Object>> toResponseEntity() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        
        if (data != null) {
            if (data instanceof Map) {
                response.putAll((Map<String, Object>) data);
            } else {
                response.put("data", data);
            }
        }
        
        if (message != null) {
            response.put("message", message);
        }
        
        return success ? 
            ResponseEntity.ok(response) : 
            ResponseEntity.internalServerError().body(response);
    }
    
    /**
     * 간편한 정적 메서드들
     */
    public static ResponseEntity<Map<String, Object>> ok(Object data) {
        return success(data).toResponseEntity();
    }
    
    public static ResponseEntity<Map<String, Object>> ok() {
        return success().toResponseEntity();
    }
    
    public static ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }
    
    public static ResponseEntity<Map<String, Object>> internalError(String message) {
        return error(message).toResponseEntity();
    }
}