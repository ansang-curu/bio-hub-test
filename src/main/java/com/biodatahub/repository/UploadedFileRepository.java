package com.biodatahub.repository;

import com.biodatahub.model.UploadedFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UploadedFileRepository {
    
    void insertFile(UploadedFile uploadedFile);
    
    UploadedFile findByFileId(@Param("fileId") String fileId);
    
    UploadedFile findById(@Param("id") Long id);
    
    List<UploadedFile> findAll();
    
    List<UploadedFile> findByStatus(@Param("status") UploadedFile.UploadStatus status);
    
    List<UploadedFile> findRecentFiles(@Param("limit") int limit);
    
    void updateUploadStatus(
        @Param("fileId") String fileId, 
        @Param("status") UploadedFile.UploadStatus status
    );
    
    void updateFilePath(
        @Param("fileId") String fileId,
        @Param("filePath") String filePath
    );
    
    void deleteByFileId(@Param("fileId") String fileId);
    
    void deleteById(@Param("id") Long id);
    
    int countByStatus(@Param("status") UploadedFile.UploadStatus status);
    
    Long getTotalUploadedSize();
}