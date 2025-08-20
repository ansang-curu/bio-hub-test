package com.biodatahub.repository;

import com.biodatahub.model.SequenceData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SequenceRepository {
    
    void insertSequence(SequenceData sequenceData);
    
    void insertSequenceBatch(List<SequenceData> sequences);
    
    SequenceData findById(@Param("id") Long id);
    
    List<SequenceData> findByFileId(@Param("fileId") String fileId);
    
    List<SequenceData> findByFileIdWithPaging(
        @Param("fileId") String fileId, 
        @Param("offset") int offset, 
        @Param("limit") int limit
    );
    
    int countByFileId(@Param("fileId") String fileId);
    
    int countValidSequencesByFileId(@Param("fileId") String fileId);
    
    List<SequenceData> findByGcContentRange(
        @Param("fileId") String fileId,
        @Param("minGc") Double minGc,
        @Param("maxGc") Double maxGc
    );
    
    List<SequenceData> findByLengthRange(
        @Param("fileId") String fileId,
        @Param("minLength") Integer minLength,
        @Param("maxLength") Integer maxLength
    );
    
    Double getAverageGcContent(@Param("fileId") String fileId);
    
    Double getAverageSequenceLength(@Param("fileId") String fileId);
    
    Integer getMinSequenceLength(@Param("fileId") String fileId);
    
    Integer getMaxSequenceLength(@Param("fileId") String fileId);
    
    Long getTotalSequenceLength(@Param("fileId") String fileId);
    
    
    void updateSequence(SequenceData sequenceData);
    
    void deleteByFileId(@Param("fileId") String fileId);
    
    void deleteById(@Param("id") Long id);
}