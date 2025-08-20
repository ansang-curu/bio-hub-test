package com.biodatahub.dto;

import lombok.*;

/**
 * 염기 구성 정보 DTO
 */
@Value
@Builder
public class BaseCompositionDto {
    long aCount;
    long tCount;
    long cCount;
    long gCount;
    long nCount;
    long totalCount;
    
    public double getGcContent() {
        return totalCount > 0 ? (double) (gCount + cCount) / totalCount * 100 : 0.0;
    }
    
    public double getAtContent() {
        return totalCount > 0 ? (double) (aCount + tCount) / totalCount * 100 : 0.0;
    }
}