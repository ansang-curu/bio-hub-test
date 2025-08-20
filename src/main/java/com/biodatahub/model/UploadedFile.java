package com.biodatahub.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFile {
    
    private Long id;
    private String fileId;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private UploadStatus uploadStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum UploadStatus {
        UPLOADING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";
        
        long size = fileSize;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return size + " " + units[unitIndex];
    }
}