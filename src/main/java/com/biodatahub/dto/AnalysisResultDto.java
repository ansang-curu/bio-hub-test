package com.biodatahub.dto;

import lombok.*;
import java.util.Map;

/**
 * 분석 결과를 담는 DTO
 */
@Value
@Builder
public class AnalysisResultDto {
    String fileId;
    Integer totalSequences;
    Integer validSequences;
    Long totalLength;
    Double averageLength;
    Integer minLength;
    Integer maxLength;
    Double averageGcContent;
    Integer n90;
    
    // 분포 데이터는 Map으로 유지 (동적 데이터)
    Map<String, Integer> lengthDistribution;
    Map<String, Integer> gcDistribution;
    Map<String, Long> baseComposition;
}