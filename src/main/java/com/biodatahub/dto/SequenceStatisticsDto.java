package com.biodatahub.dto;

import lombok.*;

/**
 * 서열 기본 통계 정보 DTO
 */
@Value
@Builder
public class SequenceStatisticsDto {
    int totalSequences;
    int validSequences;
    long totalLength;
    double averageLength;
    int minLength;
    int maxLength;
    double averageGcContent;
    
    // 염기 구성
    BaseCompositionDto baseComposition;
    
    // 분포 정보
    DistributionDto lengthDistribution;
    DistributionDto gcDistribution;
}