package com.biodatahub.model;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequenceMatch {
    
    private SequenceData referenceSequence;
    private SequenceData comparisonSequence;
    private String comparisonFileId;
    private String comparisonFileName;
    
    // 비교 결과
    private int lengthDifference;
    private double lengthRatio;
    private double gcContentDifference;
    private Map<String, Long> baseCompositionDifferences;
    
    // 유사도 점수
    private double similarityScore;
    private String similarityGrade;
}