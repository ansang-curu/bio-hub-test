package com.biodatahub.controller;

import com.biodatahub.service.SequenceAnalysisService;
import com.biodatahub.dto.SequenceStatisticsDto;
import com.biodatahub.common.ApiResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final SequenceAnalysisService sequenceAnalysisService;

    @PostMapping("/analyze/{fileId}")
    public ResponseEntity<Map<String, Object>> analyzeFile(@PathVariable String fileId) {
        try {
            log.info("Analyzing file: {}", fileId);
            
            SequenceStatisticsDto results = sequenceAnalysisService.analyzeFile(fileId);
            
            Map<String, Object> data = Map.of(
                "fileId", fileId,
                "statistics", results
            );
            
            return ApiResponse.ok(data);
            
        } catch (Exception e) {
            log.error("Error analyzing file: {}", fileId, e);
            return ApiResponse.internalError("Failed to analyze file: " + e.getMessage());
        }
    }
}