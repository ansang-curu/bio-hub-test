package com.biodatahub.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

/**
 * 서열 비교 결과 DTO
 */
@Value
@Builder
public class ComparisonResultDto {
    String referenceFileId;
    List<String> comparisonFileIds;
    
    // 전체 비교 결과
    ComparisonSummaryDto summary;
    
    // 파일별 비교 결과
    Map<String, FileComparisonDto> fileComparisons;
    
    @Value
    @Builder
    public static class ComparisonSummaryDto {
        int totalComparisons;
        double averageSimilarity;
        double minSimilarity;
        double maxSimilarity;
        Map<String, Integer> similarityDistribution;
    }
    
    @Value
    @Builder
    public static class FileComparisonDto {
        String fileId;
        double averageSimilarity;
        int totalMatches;
        BaseCompositionDto compositionDifference;
        List<SequenceMatchDto> topMatches;
    }
    
    @Value
    @Builder
    public static class SequenceMatchDto {
        String referenceHeader;
        String comparisonHeader;
        double similarity;
        int alignmentLength;
        int matches;
        int mismatches;
    }
}