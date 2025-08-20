package com.biodatahub.dto;

import lombok.*;

/**
 * 파일 업로드 결과를 담는 DTO
 */
@Value
@Builder
public class FileUploadResultDto {
    boolean success;
    String fileId;
    String originalName;
    String fileName;
    String filePath;
    Long fileSize;
    String status;
    String message;
    Boolean isValid;
    
    public static FileUploadResultDto success(String fileId, String originalName, String filePath, long fileSize) {
        return FileUploadResultDto.builder()
            .success(true)
            .fileId(fileId)
            .originalName(originalName)
            .fileName(originalName)
            .filePath(filePath)
            .fileSize(fileSize)
            .status("COMPLETED")
            .message("File uploaded successfully")
            .isValid(true)
            .build();
    }
    
    public static FileUploadResultDto failure(String message) {
        return FileUploadResultDto.builder()
            .success(false)
            .message(message)
            .isValid(false)
            .build();
    }
}