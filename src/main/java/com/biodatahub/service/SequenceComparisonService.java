package com.biodatahub.service;

import com.biodatahub.model.SequenceData;
import com.biodatahub.model.SequenceMatch;
import com.biodatahub.model.UploadedFile;
import com.biodatahub.repository.SequenceRepository;
import com.biodatahub.repository.UploadedFileRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.biodatahub.dto.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SequenceComparisonService {

    private final SequenceRepository sequenceRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final SequenceAnalysisService analysisService;
    
    // 분석 결과를 임시 저장하는 캐시
    private final Map<String, Map<String, Object>> resultCache = new ConcurrentHashMap<>();
    // 진행 중인 분석 작업을 추적하는 맵
    private final Map<String, CompletableFuture<Map<String, Object>>> runningTasks = new ConcurrentHashMap<>();

    public CompletableFuture<Map<String, Object>> compareSequences(String referenceId, List<String> comparisonIds) {
        log.info("=== CompletableFuture.supplyAsync called ===");
        log.info("Creating async task for referenceId: {}, comparisonIds: {}", referenceId, comparisonIds);
        
        String cacheKey = generateCacheKey(referenceId, comparisonIds);
        
        // 이미 진행 중인 작업이 있는지 확인
        CompletableFuture<Map<String, Object>> existingTask = runningTasks.get(cacheKey);
        if (existingTask != null && !existingTask.isDone()) {
            log.info("Task already running for cache key: {}", cacheKey);
            return existingTask;
        }
        
        // 이미 완료된 결과가 있는지 확인
        Map<String, Object> cachedResult = resultCache.get(cacheKey);
        if (cachedResult != null) {
            log.info("Result already cached for cache key: {}", cacheKey);
            return CompletableFuture.completedFuture(cachedResult);
        }
        
        CompletableFuture<Map<String, Object>> task = CompletableFuture.supplyAsync(() -> {
            try {
                log.info("=== ASYNC TASK STARTED ===");
                log.info("Starting sequence comparison analysis - Reference: {}, Comparisons: {}", 
                        referenceId, comparisonIds);

                // 1. 모든 파일이 업로드 완료되었는지 확인
                validateFilesUploaded(referenceId, comparisonIds);

                // 2. 각 파일의 서열 데이터 분석
                SequenceStatisticsDto referenceAnalysis = getOrCreateAnalysis(referenceId);
                
                Map<String, SequenceStatisticsDto> comparisonAnalyses = new HashMap<>();
                for (String comparisonId : comparisonIds) {
                    comparisonAnalyses.put(comparisonId, getOrCreateAnalysis(comparisonId));
                }

                // 3. 서열 비교 수행
                Map<String, Object> comparisonResults = performSequenceComparison(
                        referenceId, referenceAnalysis, comparisonIds, comparisonAnalyses);

                // 4. 결과 캐시에 저장
                resultCache.put(cacheKey, comparisonResults);

                log.info("서열 비교가 성공적으로 완료되었습니다");
                return comparisonResults;

            } catch (Exception e) {
                log.error("서열 비교 중 오류 발생", e);
                throw new RuntimeException("비교 분석 실패: " + e.getMessage());
            } finally {
                // 완료된 작업 제거
                runningTasks.remove(cacheKey);
            }
        });
        
        // 진행 중인 작업 목록에 추가
        runningTasks.put(cacheKey, task);
        log.info("작업이 실행 목록에 추가되었습니다. Cache key: {}", cacheKey);
        
        return task;
    }

    private void validateFilesUploaded(String referenceId, List<String> comparisonIds) {
        // 기준 파일 확인
        UploadedFile referenceFile = uploadedFileRepository.findByFileId(referenceId);
        if (referenceFile == null || referenceFile.getUploadStatus() != UploadedFile.UploadStatus.COMPLETED) {
            throw new RuntimeException("기준 파일이 업로드되지 않았거나 완료되지 않음: " + referenceId);
        }

        // 비교 파일들 확인
        for (String comparisonId : comparisonIds) {
            UploadedFile comparisonFile = uploadedFileRepository.findByFileId(comparisonId);
            if (comparisonFile == null || comparisonFile.getUploadStatus() != UploadedFile.UploadStatus.COMPLETED) {
                throw new RuntimeException("비교 파일이 업로드되지 않았거나 완료되지 않음: " + comparisonId);
            }
        }
    }

    private SequenceStatisticsDto getOrCreateAnalysis(String fileId) {
        try {
            // 기존 분석 결과가 있는지 확인
            SequenceStatisticsDto existingAnalysis = analysisService.getBasicStatistics(fileId);
            
            if (existingAnalysis != null && existingAnalysis.getTotalSequences() > 0) {
                return existingAnalysis;
            }

            // 기존 분석이 없으면 새로 분석
            log.info("Creating new analysis for fileId: {}", fileId);
            return analysisService.analyzeFile(fileId);

        } catch (Exception e) {
            log.error("Error getting analysis for fileId: {}", fileId, e);
            throw new RuntimeException("Failed to analyze file: " + fileId);
        }
    }

    private Map<String, Object> performSequenceComparison(
            String referenceId, SequenceStatisticsDto referenceAnalysis,
            List<String> comparisonIds, Map<String, SequenceStatisticsDto> comparisonAnalyses) {

        Map<String, Object> results = new HashMap<>();

        try {
            // 1. 기준 파일의 개별 서열들 가져오기
            List<SequenceData> referenceSequences = sequenceRepository.findByFileId(referenceId);
            log.info("Found {} reference sequences", referenceSequences.size());

            // 2. 비교 파일들의 개별 서열들 가져오기
            Map<String, List<SequenceData>> comparisonSequencesMap = new HashMap<>();
            for (String comparisonId : comparisonIds) {
                List<SequenceData> comparisonSequences = sequenceRepository.findByFileId(comparisonId);
                comparisonSequencesMap.put(comparisonId, comparisonSequences);
                log.info("Found {} sequences in comparison file {}", comparisonSequences.size(), comparisonId);
            }

            // 3. 기준 파일 정보
            UploadedFile referenceFile = uploadedFileRepository.findByFileId(referenceId);
            results.put("referenceFile", Map.of(
                    "fileId", referenceId,
                    "fileName", referenceFile.getOriginalName(),
                    "totalSequences", referenceSequences.size(),
                    "sequences", referenceSequences.stream().map(this::convertSequenceToMap).collect(Collectors.toList())
            ));

            // 4. 서열별 비교 수행
            List<Map<String, Object>> sequenceComparisons = performSequenceBySequenceComparison(
                    referenceSequences, comparisonSequencesMap, comparisonIds);
            results.put("sequenceComparisons", sequenceComparisons);

            // 5. 전체 요약 통계
            Map<String, Object> summaryStats = calculateSequenceComparisonSummary(sequenceComparisons);
            results.put("summaryStats", summaryStats);

            return results;

        } catch (Exception e) {
            log.error("Error performing sequence comparison", e);
            throw new RuntimeException("Failed to perform sequence comparison: " + e.getMessage());
        }
    }

    private Map<String, Object> convertSequenceToMap(SequenceData sequence) {
        Map<String, Object> seqMap = new HashMap<>();
        seqMap.put("sequenceId", sequence.getSequenceId());
        seqMap.put("header", sequence.getHeader());
        seqMap.put("length", sequence.getSequenceLength());
        seqMap.put("gcContent", sequence.getGcContent());
        seqMap.put("aCount", sequence.getACount());
        seqMap.put("tCount", sequence.getTCount());
        seqMap.put("cCount", sequence.getCCount());
        seqMap.put("gCount", sequence.getGCount());
        seqMap.put("nCount", sequence.getNCount());
        seqMap.put("isValid", sequence.getIsValid());
        return seqMap;
    }

    private List<Map<String, Object>> performSequenceBySequenceComparison(
            List<SequenceData> referenceSequences,
            Map<String, List<SequenceData>> comparisonSequencesMap,
            List<String> comparisonIds) {

        List<Map<String, Object>> sequenceComparisons = new ArrayList<>();

        // 각 기준 서열에 대해
        for (SequenceData refSeq : referenceSequences) {
            Map<String, Object> refComparison = new HashMap<>();
            refComparison.put("referenceSequence", convertSequenceToMap(refSeq));
            
            List<Map<String, Object>> matches = new ArrayList<>();

            // 모든 비교 파일의 서열들과 비교
            List<SequenceMatch> allMatches = new ArrayList<>();
            
            for (String comparisonId : comparisonIds) {
                UploadedFile comparisonFile = uploadedFileRepository.findByFileId(comparisonId);
                List<SequenceData> comparisonSequences = comparisonSequencesMap.get(comparisonId);

                // 모든 서열과 비교하여 결과 수집
                for (SequenceData compSeq : comparisonSequences) {
                    SequenceMatch match = compareSequences(refSeq, compSeq, comparisonId, comparisonFile.getOriginalName());
                    allMatches.add(match);
                }
            }
            
            // 유사도 점수 높은 순으로 정렬
            allMatches.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
            
            // 정렬된 모든 매치를 Map으로 변환
            List<Map<String, Object>> sortedMatches = allMatches.stream()
                    .map(this::convertMatchToMap)
                    .collect(Collectors.toList());
            
            refComparison.put("allMatches", sortedMatches);
            refComparison.put("totalMatches", allMatches.size());

            refComparison.put("matches", matches);
            sequenceComparisons.add(refComparison);
        }

        return sequenceComparisons;
    }

    private SequenceMatch findBestMatch(SequenceData referenceSeq, List<SequenceData> comparisonSequences, 
                                      String comparisonFileId, String comparisonFileName) {
        SequenceMatch bestMatch = null;
        double bestSimilarity = -1.0;

        for (SequenceData compSeq : comparisonSequences) {
            SequenceMatch match = compareSequences(referenceSeq, compSeq, comparisonFileId, comparisonFileName);
            if (match.getSimilarityScore() > bestSimilarity) {
                bestSimilarity = match.getSimilarityScore();
                bestMatch = match;
            }
        }

        return bestMatch;
    }

    private SequenceMatch compareSequences(SequenceData refSeq, SequenceData compSeq, 
                                         String comparisonFileId, String comparisonFileName) {
        SequenceMatch match = new SequenceMatch();
        match.setReferenceSequence(refSeq);
        match.setComparisonSequence(compSeq);
        match.setComparisonFileId(comparisonFileId);
        match.setComparisonFileName(comparisonFileName);

        // 길이 차이
        int lengthDiff = compSeq.getSequenceLength() - refSeq.getSequenceLength();
        match.setLengthDifference(lengthDiff);
        match.setLengthRatio(refSeq.getSequenceLength() > 0 ? 
                (double) compSeq.getSequenceLength() / refSeq.getSequenceLength() : 0.0);


        // 염기 조성 차이
        Map<String, Long> baseCompDiff = new HashMap<>();
        baseCompDiff.put("A", (compSeq.getACount() != null ? compSeq.getACount() : 0L) - 
                            (refSeq.getACount() != null ? refSeq.getACount() : 0L));
        baseCompDiff.put("T", (compSeq.getTCount() != null ? compSeq.getTCount() : 0L) - 
                            (refSeq.getTCount() != null ? refSeq.getTCount() : 0L));
        baseCompDiff.put("C", (compSeq.getCCount() != null ? compSeq.getCCount() : 0L) - 
                            (refSeq.getCCount() != null ? refSeq.getCCount() : 0L));
        baseCompDiff.put("G", (compSeq.getGCount() != null ? compSeq.getGCount() : 0L) - 
                            (refSeq.getGCount() != null ? refSeq.getGCount() : 0L));
        baseCompDiff.put("N", (compSeq.getNCount() != null ? compSeq.getNCount() : 0L) - 
                            (refSeq.getNCount() != null ? refSeq.getNCount() : 0L));
        match.setBaseCompositionDifferences(baseCompDiff);

        // 유사도 점수 계산
        double similarityScore = calculateSequenceSimilarity(refSeq, compSeq);
        match.setSimilarityScore(similarityScore);
        match.setSimilarityGrade(getSimilarityGrade(similarityScore));

        return match;
    }

    private double calculateSequenceSimilarity(SequenceData refSeq, SequenceData compSeq) {
        // 순수 서열 비교만 사용 - 실제 염기 일치도 계산
        return calculateSequenceMatchPercentage(refSeq.getSequence(), compSeq.getSequence());
    }
    
    /**
     * 실제 서열 문자열을 비교하여 일치하는 염기의 비율을 계산
     * 예: GCGCGC vs GCAAAA = 2/6 = 33.33%
     */
    private double calculateSequenceMatchPercentage(String refSequence, String compSequence) {
        if (refSequence == null || compSequence == null || refSequence.isEmpty() || compSequence.isEmpty()) {
            return 0.0;
        }
        
        // 두 서열을 대문자로 변환
        String ref = refSequence.toUpperCase();
        String comp = compSequence.toUpperCase();
        
        // 짧은 길이를 기준으로 비교 (정렬 없이 단순 위치별 비교)
        int minLength = Math.min(ref.length(), comp.length());
        int matches = 0;
        
        for (int i = 0; i < minLength; i++) {
            if (ref.charAt(i) == comp.charAt(i)) {
                matches++;
            }
        }
        
        // 일치 비율 계산 (긴 서열 길이 기준)
        int maxLength = Math.max(ref.length(), comp.length());
        return (double) matches / maxLength * 100.0;
    }

    private Map<String, Object> convertMatchToMap(SequenceMatch match) {
        Map<String, Object> matchMap = new HashMap<>();
        matchMap.put("comparisonSequence", convertSequenceToMap(match.getComparisonSequence()));
        matchMap.put("comparisonFileId", match.getComparisonFileId());
        matchMap.put("comparisonFileName", match.getComparisonFileName());
        matchMap.put("lengthDifference", match.getLengthDifference());
        matchMap.put("lengthRatio", match.getLengthRatio());
        matchMap.put("baseCompositionDifferences", match.getBaseCompositionDifferences());
        matchMap.put("similarityScore", match.getSimilarityScore());
        matchMap.put("similarityGrade", match.getSimilarityGrade());
        return matchMap;
    }

    private Map<String, Object> calculateSequenceComparisonSummary(List<Map<String, Object>> sequenceComparisons) {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("totalReferenceSequences", sequenceComparisons.size());
        
        // 모든 유사도 점수 수집
        List<Double> allSimilarityScores = new ArrayList<>();
        int totalComparisons = 0;
        
        for (Map<String, Object> seqComp : sequenceComparisons) {
            List<Map<String, Object>> allMatches = (List<Map<String, Object>>) seqComp.get("allMatches");
            if (allMatches != null) {
                totalComparisons += allMatches.size();
                for (Map<String, Object> match : allMatches) {
                    Double similarityScore = (Double) match.get("similarityScore");
                    if (similarityScore != null) {
                        allSimilarityScores.add(similarityScore);
                    }
                }
            }
        }
        
        if (!allSimilarityScores.isEmpty()) {
            double avgSimilarity = allSimilarityScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double maxSimilarity = allSimilarityScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double minSimilarity = allSimilarityScores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            
            summary.put("averageSimilarity", avgSimilarity);
            summary.put("maxSimilarity", maxSimilarity);
            summary.put("minSimilarity", minSimilarity);
            summary.put("totalComparisons", totalComparisons);
            
            // 유사도 구간별 분포
            Map<String, Integer> similarityDistribution = new HashMap<>();
            similarityDistribution.put("very_high", (int) allSimilarityScores.stream().filter(s -> s >= 90).count());
            similarityDistribution.put("high", (int) allSimilarityScores.stream().filter(s -> s >= 75 && s < 90).count());
            similarityDistribution.put("medium", (int) allSimilarityScores.stream().filter(s -> s >= 60 && s < 75).count());
            similarityDistribution.put("low", (int) allSimilarityScores.stream().filter(s -> s >= 40 && s < 60).count());
            similarityDistribution.put("very_low", (int) allSimilarityScores.stream().filter(s -> s < 40).count());
            
            summary.put("similarityDistribution", similarityDistribution);
        } else {
            summary.put("averageSimilarity", 0.0);
            summary.put("maxSimilarity", 0.0);
            summary.put("minSimilarity", 0.0);
            summary.put("totalComparisons", 0);
            summary.put("similarityDistribution", Map.of("very_high", 0, "high", 0, "medium", 0, "low", 0, "very_low", 0));
        }
        
        return summary;
    }

    private Map<String, Object> calculateSequenceDifferences(
            Map<String, Object> referenceAnalysis, Map<String, Object> comparisonAnalysis) {
        
        Map<String, Object> differences = new HashMap<>();

        // 서열 수 차이
        int refSeqCount = (Integer) referenceAnalysis.get("totalSequences");
        int compSeqCount = (Integer) comparisonAnalysis.get("totalSequences");
        differences.put("sequenceCountDiff", compSeqCount - refSeqCount);
        differences.put("sequenceCountRatio", refSeqCount > 0 ? (double) compSeqCount / refSeqCount : 0.0);

        // 총 길이 차이
        long refTotalLength = (Long) referenceAnalysis.get("totalLength");
        long compTotalLength = (Long) comparisonAnalysis.get("totalLength");
        differences.put("totalLengthDiff", compTotalLength - refTotalLength);
        differences.put("totalLengthRatio", refTotalLength > 0 ? (double) compTotalLength / refTotalLength : 0.0);

        // 평균 길이 차이
        double refAvgLength = (Double) referenceAnalysis.get("averageLength");
        double compAvgLength = (Double) comparisonAnalysis.get("averageLength");
        differences.put("averageLengthDiff", compAvgLength - refAvgLength);
        differences.put("averageLengthRatio", refAvgLength > 0 ? compAvgLength / refAvgLength : 0.0);

        // GC 함량 차이
        double refGcContent = (Double) referenceAnalysis.get("averageGcContent");
        double compGcContent = (Double) comparisonAnalysis.get("averageGcContent");
        differences.put("gcContentDiff", compGcContent - refGcContent);


        // 염기 조성 차이
        Map<String, Object> refBaseComp = (Map<String, Object>) referenceAnalysis.get("baseComposition");
        Map<String, Object> compBaseComp = (Map<String, Object>) comparisonAnalysis.get("baseComposition");
        
        Map<String, Object> baseCompDiff = new HashMap<>();
        for (String base : Arrays.asList("A", "T", "C", "G", "N")) {
            long refCount = (Long) refBaseComp.get(base);
            long compCount = (Long) compBaseComp.get(base);
            baseCompDiff.put(base + "Diff", compCount - refCount);
            baseCompDiff.put(base + "Ratio", refCount > 0 ? (double) compCount / refCount : 0.0);
        }
        differences.put("baseCompositionDiff", baseCompDiff);

        // 차이 등급 계산 (유사도 점수)
        double similarityScore = calculateSimilarityScore(differences);
        differences.put("similarityScore", similarityScore);
        differences.put("similarityGrade", getSimilarityGrade(similarityScore));

        return differences;
    }

    private double calculateSimilarityScore(Map<String, Object> differences) {
        double score = 100.0; // 최대 점수에서 시작

        // 서열 수 비율에 따른 점수 차감
        double seqCountRatio = (Double) differences.get("sequenceCountRatio");
        score -= Math.abs(1.0 - seqCountRatio) * 20;

        // 평균 길이 비율에 따른 점수 차감
        double avgLengthRatio = (Double) differences.get("averageLengthRatio");
        score -= Math.abs(1.0 - avgLengthRatio) * 20;

        // GC 함량 차이에 따른 점수 차감
        double gcContentDiff = (Double) differences.get("gcContentDiff");
        score -= Math.abs(gcContentDiff) * 2;


        return Math.max(0, Math.min(100, score));
    }

    private String getSimilarityGrade(double score) {
        if (score >= 90) return "매우 유사";
        else if (score >= 75) return "유사";
        else if (score >= 60) return "보통";
        else if (score >= 40) return "다소 다름";
        else return "매우 다름";
    }

    private Map<String, Object> calculateOverallComparisonStats(
            Map<String, Object> referenceAnalysis, Collection<Map<String, Object>> comparisonAnalyses) {
        
        Map<String, Object> overallStats = new HashMap<>();

        // 비교 파일 수
        overallStats.put("comparisonFileCount", comparisonAnalyses.size());

        // 전체 서열 수 통계
        List<Integer> allSeqCounts = comparisonAnalyses.stream()
                .map(analysis -> (Integer) analysis.get("totalSequences"))
                .collect(Collectors.toList());
        
        overallStats.put("totalSequencesStats", Map.of(
                "min", allSeqCounts.stream().min(Integer::compareTo).orElse(0),
                "max", allSeqCounts.stream().max(Integer::compareTo).orElse(0),
                "average", allSeqCounts.stream().mapToDouble(Integer::doubleValue).average().orElse(0.0)
        ));

        // 전체 길이 통계
        List<Long> allTotalLengths = comparisonAnalyses.stream()
                .map(analysis -> (Long) analysis.get("totalLength"))
                .collect(Collectors.toList());
        
        overallStats.put("totalLengthStats", Map.of(
                "min", allTotalLengths.stream().min(Long::compareTo).orElse(0L),
                "max", allTotalLengths.stream().max(Long::compareTo).orElse(0L),
                "average", allTotalLengths.stream().mapToDouble(Long::doubleValue).average().orElse(0.0)
        ));

        // GC 함량 통계
        List<Double> allGcContents = comparisonAnalyses.stream()
                .map(analysis -> (Double) analysis.get("averageGcContent"))
                .collect(Collectors.toList());
        
        overallStats.put("gcContentStats", Map.of(
                "min", allGcContents.stream().min(Double::compareTo).orElse(0.0),
                "max", allGcContents.stream().max(Double::compareTo).orElse(0.0),
                "average", allGcContents.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
        ));

        return overallStats;
    }

    public Map<String, Object> getComparisonResults(String referenceId, List<String> comparisonIds) {
        log.info("=== 서비스: getComparisonResults ===");
        log.info("참조 ID: {}", referenceId);
        log.info("비교 IDs: {}", comparisonIds);
        
        String cacheKey = generateCacheKey(referenceId, comparisonIds);
        log.info("생성된 캐시 키: {}", cacheKey);
        log.info("사용 가능한 캐시 키들: {}", resultCache.keySet());
        
        // 먼저 캐시에서 결과 확인
        Map<String, Object> result = resultCache.get(cacheKey);
        if (result != null) {
            log.info("캐시에서 결과 발견");
            return result;
        }
        
        // 진행 중인 작업이 있는지 확인
        CompletableFuture<Map<String, Object>> runningTask = runningTasks.get(cacheKey);
        if (runningTask != null) {
            if (runningTask.isDone()) {
                try {
                    if (runningTask.isCompletedExceptionally()) {
                        log.error("비동기 작업이 예외와 함께 완료됨: {}", cacheKey);
                        runningTasks.remove(cacheKey); // 실패한 작업 제거
                        return null;
                    } else {
                        Map<String, Object> taskResult = runningTask.get();
                        log.info("비동기 작업이 방금 완료됨, 결과 반환");
                        return taskResult;
                    }
                } catch (Exception e) {
                    log.error("비동기 작업 결과 조회 중 오류", e);
                    runningTasks.remove(cacheKey);
                    return null;
                }
            } else {
                log.info("작업이 여전히 진행 중입니다");
                return null;
            }
        }
        
        log.info("캐시 결과 없음 및 진행 중인 작업 없음");
        return null;
    }

    private String generateCacheKey(String referenceId, List<String> comparisonIds) {
        List<String> sortedIds = new ArrayList<>(comparisonIds);
        Collections.sort(sortedIds);
        return referenceId + "_" + String.join("_", sortedIds);
    }
}