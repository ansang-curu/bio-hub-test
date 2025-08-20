package com.biodatahub.controller;

import com.biodatahub.service.FileProcessingService;
import com.biodatahub.dto.FileUploadResultDto;
import com.biodatahub.common.ApiResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FileUploadController {

    private final FileProcessingService fileProcessingService;
    
    @Value("${biodatahub.file.allowed-extensions}")
    private String allowedExtensions;


    @PostMapping("/single")
    public ResponseEntity<Map<String, Object>> uploadSingleFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileId", required = false) String fileId,
            HttpServletRequest request) {
        
        try {
            log.info("=== Single file upload request received ===");
            log.info("File name: {}", file.getOriginalFilename());
            log.info("File size: {}", file.getSize());
            log.info("Received fileId parameter: {}", fileId);
            
            // Log all request parameters for debugging
            log.info("All request parameters:");
            request.getParameterMap().forEach((key, values) -> {
                log.info("  {}: {}", key, String.join(", ", values));
            });
            
            // Validate file
            if (file.isEmpty()) {
                return ApiResponse.badRequest("File is empty");
            }
            
            if (!isValidFileExtension(file.getOriginalFilename())) {
                return ApiResponse.badRequest("Invalid file extension. Allowed: " + allowedExtensions);
            }
            
            // Generate fileId if not provided
            if (fileId == null || fileId.trim().isEmpty()) {
                fileId = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 9);
                log.info("Generated new fileId: {}", fileId);
            } else {
                log.info("Using provided fileId: {}", fileId);
            }
            
            String savedPath = fileProcessingService.processSingleFile(file, fileId);
            
            FileUploadResultDto result = FileUploadResultDto.success(
                fileId, file.getOriginalFilename(), savedPath, file.getSize());
            
            log.info("Single file uploaded: {} (ID: {})", file.getOriginalFilename(), fileId);
            return ApiResponse.ok(result);
            
        } catch (IOException e) {
            log.error("Error uploading single file: {}", file.getOriginalFilename(), e);
            return ApiResponse.internalError("Error uploading file: " + e.getMessage());
        }
    }


    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateFile(
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") long fileSize) {
        
        // Validate file extension
        if (!isValidFileExtension(fileName)) {
            Map<String, Object> result = Map.of(
                "valid", false,
                "message", "Invalid file extension. Allowed: " + allowedExtensions
            );
            return ResponseEntity.badRequest().body(result);
        }
        
        // Validate file size (50MB limit)
        long maxSize = 50L * 1024 * 1024; // 50MB
        if (fileSize > maxSize) {
            Map<String, Object> result = Map.of(
                "valid", false,
                "message", "File size exceeds 50MB limit"
            );
            return ResponseEntity.badRequest().body(result);
        }
        
        Map<String, Object> result = Map.of(
            "valid", true,
            "message", "File is valid for upload"
        );
        return ApiResponse.ok(result);
    }

    private boolean isValidFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        
        String lowerFileName = fileName.toLowerCase();
        String[] extensions = allowedExtensions.split(",");
        
        for (String ext : extensions) {
            if (lowerFileName.endsWith(ext.trim())) {
                return true;
            }
        }
        
        return false;
    }
}