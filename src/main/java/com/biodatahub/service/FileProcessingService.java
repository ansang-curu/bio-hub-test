package com.biodatahub.service;

import com.biodatahub.model.UploadedFile;
import com.biodatahub.repository.UploadedFileRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessingService {

    private final UploadedFileRepository uploadedFileRepository;
    
    @Value("${biodatahub.file.upload-dir:uploads/fasta}")
    private String uploadDir;
    
    @Value("${biodatahub.file.temp-dir:temp}")
    private String tempDir;
    

    public String processSingleFile(MultipartFile file, String fileId) throws IOException {
        Path uploadPath = createUploadDirectory();
        String fileName = sanitizeFileName(file.getOriginalFilename());
        Path filePath = uploadPath.resolve(fileId + "_" + fileName);
        
        // Save uploaded file record to database
        UploadedFile uploadedFile = UploadedFile.builder()
                .fileId(fileId)
                .originalName(file.getOriginalFilename())
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .fileType("FASTA")
                .uploadStatus(UploadedFile.UploadStatus.UPLOADING)
                .build();
        
        uploadedFileRepository.insertFile(uploadedFile);
        
        // Save file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Update status to completed
        uploadedFileRepository.updateUploadStatus(fileId, UploadedFile.UploadStatus.COMPLETED);
        
        log.info("Single file saved: {}", filePath.toString());
        return filePath.toString();
    }



    private Path createUploadDirectory() throws IOException {
        Path path = Paths.get(uploadDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    private Path createTempDirectory() throws IOException {
        Path path = Paths.get(tempDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unknown";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}