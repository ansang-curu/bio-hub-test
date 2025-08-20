package com.biodatahub.controller;

import com.biodatahub.service.SequenceComparisonService;
import com.biodatahub.common.ApiResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ComparisonController {

    private final SequenceComparisonService comparisonService;

    @GetMapping("/comparison-analysis")
    public String comparisonAnalysisPage() {
        return "comparison-analysis";
    }

    @PostMapping("/api/comparison/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startComparison(
            @RequestParam("referenceId") String referenceId,
            @RequestParam("comparisonIds") String comparisonIdsParam) {
        
        try {
            log.info("Starting sequence comparison - Reference: {}, ComparisonIds: {}", 
                    referenceId, comparisonIdsParam);
            
            // 쉼표로 분리된 문자열을 List로 변환
            List<String> comparisonIds = java.util.Arrays.asList(comparisonIdsParam.split(","));
            log.info("Parsed comparisonIds: {}", comparisonIds);
            
            // 비동기로 비교 분석 시작
            log.info("=== Starting async comparison analysis ===");
            CompletableFuture<Map<String, Object>> analysisResult = 
                    comparisonService.compareSequences(referenceId, comparisonIds);
            log.info("CompletableFuture created successfully");
            
            Map<String, Object> data = Map.of(
                "message", "Comparison analysis started",
                "referenceId", referenceId,
                "comparisonIds", comparisonIds
            );
            
            return ApiResponse.ok(data);
            
        } catch (Exception e) {
            log.error("Error starting comparison analysis", e);
            return ApiResponse.internalError("Failed to start comparison: " + e.getMessage());
        }
    }

    @GetMapping("/api/comparison/results")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getComparisonResults(
            @RequestParam("referenceId") String referenceId,
            @RequestParam("comparisonIds") String comparisonIdsParam) {
        
        try {
            log.info("=== Getting comparison results ===");
            log.info("Reference ID: {}", referenceId);
            log.info("Comparison IDs param: {}", comparisonIdsParam);
            
            // 입력값 검증
            if (referenceId == null || referenceId.trim().isEmpty()) {
                log.error("Reference ID is null or empty");
                return ApiResponse.badRequest("Reference ID is required");
            }
            
            if (comparisonIdsParam == null || comparisonIdsParam.trim().isEmpty()) {
                log.error("Comparison IDs param is null or empty");
                return ApiResponse.badRequest("Comparison IDs are required");
            }
            
            // 쉼표로 분리된 문자열을 List로 변환
            List<String> comparisonIds = java.util.Arrays.asList(comparisonIdsParam.trim().split(","));
            log.info("Parsed comparison IDs: {}", comparisonIds);
            
            // 비교 결과 조회
            Map<String, Object> results = comparisonService.getComparisonResults(referenceId, comparisonIds);
            log.info("Retrieved results: {}", results != null ? "Found" : "Null");
            
            if (results != null && !results.isEmpty()) {
                return ApiResponse.ok(Map.of("results", results));
            } else {
                return ApiResponse.ok(Map.of("message", "Results not ready yet", "status", "processing"));
            }
            
        } catch (Exception e) {
            log.error("Error getting comparison results for referenceId: {}, comparisonIds: {}", 
                     referenceId, comparisonIdsParam, e);
            return ApiResponse.internalError("Failed to get results: " + e.getMessage());
        }
    }
}