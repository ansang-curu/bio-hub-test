package com.biodatahub.dto;

import lombok.*;
import java.util.Map;

/**
 * 분포 데이터 DTO
 */
@Value
@Builder
public class DistributionDto {
    Map<String, Integer> distribution;
    int totalSamples;
    
    public static DistributionDto of(Map<String, Integer> distribution) {
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        return DistributionDto.builder()
            .distribution(distribution)
            .totalSamples(total)
            .build();
    }
}