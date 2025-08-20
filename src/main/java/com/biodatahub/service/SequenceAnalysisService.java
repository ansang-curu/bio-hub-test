package com.biodatahub.service;

import com.biodatahub.model.SequenceData;
import com.biodatahub.model.UploadedFile;
import com.biodatahub.repository.SequenceRepository;
import com.biodatahub.repository.UploadedFileRepository;
import com.biodatahub.util.FastaParser;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import com.biodatahub.dto.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SequenceAnalysisService {

    private final SequenceRepository sequenceRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FastaParser fastaParser;

    public SequenceStatisticsDto analyzeFile(String fileId) {
        try {
            log.info("Starting analysis for fileId: {}", fileId);
                
                UploadedFile uploadedFile = uploadedFileRepository.findByFileId(fileId);
                if (uploadedFile == null) {
                    log.error("File record not found in database: {}", fileId);
                    throw new RuntimeException("File record not found in database: " + fileId);
                }
                
                log.info("Found file record: {} (status: {}, path: {})", 
                        uploadedFile.getOriginalName(), uploadedFile.getUploadStatus(), uploadedFile.getFilePath());

                if (uploadedFile.getUploadStatus() != UploadedFile.UploadStatus.COMPLETED) {
                    log.error("File upload not completed: {} (status: {})", fileId, uploadedFile.getUploadStatus());
                    throw new RuntimeException("File upload not completed: " + uploadedFile.getUploadStatus());
                }

                if (uploadedFile.getFilePath() == null || uploadedFile.getFilePath().isEmpty()) {
                    log.error("File path is empty for fileId: {}", fileId);
                    throw new RuntimeException("File path is empty for fileId: " + fileId);
                }

                File file = new File(uploadedFile.getFilePath());
                if (!file.exists()) {
                    log.error("Physical file not found: {}", uploadedFile.getFilePath());
                    throw new RuntimeException("Physical file not found: " + uploadedFile.getFilePath());
                }
                
                log.info("File validation successful, starting FASTA parsing...");

                List<SequenceData> sequences = new ArrayList<>();
                
                // Parse FASTA file and save sequences
                fastaParser.parseFileStreaming(file, sequence -> {
                    // Get sequence statistics from FastaParser
                    FastaParser.SequenceStats stats = sequence.getStats();
                    
                    SequenceData seqData = SequenceData.builder()
                            .fileId(fileId)
                            .sequenceId(extractSequenceId(sequence.getHeader()))
                            .header(sequence.getHeader())
                            .sequence(sequence.getSequence())
                            .sequenceLength(sequence.getLength())
                            .gcContent(sequence.getGcContent())
                            .aCount(stats.getACount())
                            .tCount(stats.getTCount())
                            .cCount(stats.getCCount())
                            .gCount(stats.getGCount())
                            .nCount(stats.getNCount())
                            .isValid(sequence.isValid())
                            .build();
                    
                    sequences.add(seqData);
                });

                // Save sequences to database in batches
                if (!sequences.isEmpty()) {
                    saveSequencesBatch(sequences);
                }

                // Calculate and return analysis results
                return calculateBasicStatistics(fileId, sequences);

        } catch (Exception e) {
            log.error("Error analyzing file: {}", fileId, e);
            throw new RuntimeException("Analysis failed: " + e.getMessage());
        }
    }

    private void saveSequencesBatch(List<SequenceData> sequences) {
        final int batchSize = 500; // 현실적인 배치 크기로 변경
        int totalSaved = 0;
        
        for (int i = 0; i < sequences.size(); i += batchSize) {
            int end = Math.min(i + batchSize, sequences.size());
            List<SequenceData> batch = sequences.subList(i, end);
            
            // 매우 긴 서열은 개별 처리
            List<SequenceData> largeBatch = new ArrayList<>();
            for (SequenceData seq : batch) {
                if (seq.getSequence() != null && seq.getSequence().length() > 100000) {
                    // 100KB 이상의 개별 서열은 단독 처리
                    sequenceRepository.insertSequence(seq);
                    totalSaved++;
                } else {
                    largeBatch.add(seq);
                }
            }
            
            // 일반 크기 서열들은 배치 처리
            if (!largeBatch.isEmpty()) {
                sequenceRepository.insertSequenceBatch(largeBatch);
                totalSaved += largeBatch.size();
            }
            
            // 진행률 로그 (1000개마다)
            if (totalSaved % 1000 == 0) {
                log.info("Saved {} / {} sequences", totalSaved, sequences.size());
            }
        }
        log.info("Completed: Saved {} sequences to database", totalSaved);
    }

    public SequenceStatisticsDto getBasicStatistics(String fileId) {
        return calculateBasicStatistics(fileId, null);
    }

    private SequenceStatisticsDto calculateBasicStatistics(String fileId, List<SequenceData> sequences) {
        
        try {
            // Get sequences from database if not provided
            if (sequences == null || sequences.isEmpty()) {
                sequences = sequenceRepository.findByFileId(fileId);
            }

            if (sequences.isEmpty()) {
                return createEmptyStatisticsDto();
            }

            // Basic counts
            int totalSequences = sequences.size();
            int validSequences = (int) sequences.stream().filter(SequenceData::getIsValid).count();
            
            // Length statistics
            List<Integer> lengths = sequences.stream()
                    .filter(SequenceData::getIsValid)
                    .map(SequenceData::getSequenceLength)
                    .sorted(Collections.reverseOrder())
                    .toList();
            
            long totalLength = lengths.stream().mapToLong(Integer::longValue).sum();
            double avgLength = lengths.isEmpty() ? 0 : (double) totalLength / lengths.size();
            
            // GC content statistics
            double avgGcContent = sequences.stream()
                    .filter(SequenceData::getIsValid)
                    .mapToDouble(SequenceData::getGcContent)
                    .average()
                    .orElse(0.0);
            
            // Base composition
            long totalA = sequences.stream().mapToLong(s -> s.getACount() != null ? s.getACount() : 0).sum();
            long totalT = sequences.stream().mapToLong(s -> s.getTCount() != null ? s.getTCount() : 0).sum();
            long totalC = sequences.stream().mapToLong(s -> s.getCCount() != null ? s.getCCount() : 0).sum();
            long totalG = sequences.stream().mapToLong(s -> s.getGCount() != null ? s.getGCount() : 0).sum();
            long totalN = sequences.stream().mapToLong(s -> s.getNCount() != null ? s.getNCount() : 0).sum();
            
            
            // Length distribution
            Map<String, Integer> lengthDistribution = calculateLengthDistribution(lengths);
            
            // GC content distribution
            Map<String, Integer> gcDistribution = calculateGcDistribution(sequences);
            
            // Build base composition
            BaseCompositionDto baseComposition = BaseCompositionDto.builder()
                .aCount(totalA)
                .tCount(totalT)
                .cCount(totalC)
                .gCount(totalG)
                .nCount(totalN)
                .totalCount(totalA + totalT + totalC + totalG + totalN)
                .build();
            
            // Build result
            return SequenceStatisticsDto.builder()
                .totalSequences(totalSequences)
                .validSequences(validSequences)
                .totalLength(totalLength)
                .averageLength(Math.round(avgLength * 100.0) / 100.0)
                .minLength(lengths.isEmpty() ? 0 : lengths.get(lengths.size() - 1))
                .maxLength(lengths.isEmpty() ? 0 : lengths.get(0))
                .averageGcContent(Math.round(avgGcContent * 100.0) / 100.0)
                .baseComposition(baseComposition)
                .lengthDistribution(DistributionDto.of(lengthDistribution))
                .gcDistribution(DistributionDto.of(gcDistribution))
                .build();
            
        } catch (Exception e) {
            log.error("Error calculating statistics for fileId: {}", fileId, e);
            return createEmptyStatisticsDto();
        }
    }

    private SequenceStatisticsDto createEmptyStatisticsDto() {
        BaseCompositionDto emptyComposition = BaseCompositionDto.builder()
            .aCount(0L).tCount(0L).cCount(0L).gCount(0L).nCount(0L).totalCount(0L)
            .build();
            
        return SequenceStatisticsDto.builder()
            .totalSequences(0)
            .validSequences(0)
            .totalLength(0L)
            .averageLength(0.0)
            .minLength(0)
            .maxLength(0)
            .averageGcContent(0.0)
            .baseComposition(emptyComposition)
            .lengthDistribution(DistributionDto.of(new HashMap<>()))
            .gcDistribution(DistributionDto.of(new HashMap<>()))
            .build();
    }



    private Map<String, Integer> calculateLengthDistribution(List<Integer> lengths) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        
        if (lengths.isEmpty()) return distribution;
        
        int min = lengths.get(lengths.size() - 1);
        int max = lengths.get(0);
        int binSize = Math.max(1, (max - min) / 10);
        
        for (int i = 0; i < 10; i++) {
            int binStart = min + (i * binSize);
            int binEnd = (i == 9) ? max : binStart + binSize - 1;
            String binLabel = binStart + "-" + binEnd;
            
            int count = (int) lengths.stream()
                    .filter(length -> length >= binStart && length <= binEnd)
                    .count();
            
            distribution.put(binLabel, count);
        }
        
        return distribution;
    }

    private Map<String, Integer> calculateGcDistribution(List<SequenceData> sequences) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        
        for (int i = 0; i < 10; i++) {
            double start = i * 10.0;
            double end = (i + 1) * 10.0;
            String binLabel = (int)start + "-" + (int)end + "%";
            
            int count = (int) sequences.stream()
                    .filter(SequenceData::getIsValid)
                    .filter(seq -> seq.getGcContent() >= start && seq.getGcContent() < end)
                    .count();
            
            distribution.put(binLabel, count);
        }
        
        return distribution;
    }

    private String extractSequenceId(String header) {
        if (header == null || header.isEmpty()) {
            return "unknown";
        }
        
        // Extract ID from FASTA header (first word after >)
        String[] parts = header.split("\\s+");
        return parts.length > 0 ? parts[0] : "unknown";
    }
}