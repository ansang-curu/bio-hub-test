package com.biodatahub.util;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.biodatahub.util.DNASequenceUtils.countBases;
import com.biodatahub.util.DNASequenceUtils;

@Component
@Slf4j
public class FastaParser {
    
    private static final int BUFFER_SIZE = 8192;
    private static final Pattern HEADER_PATTERN = Pattern.compile("^>\\s*(\\S+).*");
    
    public void parseFileStreaming(File file, Consumer<FastaSequence> sequenceProcessor) throws IOException {
        
        try (BufferedReader reader = new BufferedReader(
                new FileReader(file, StandardCharsets.UTF_8), BUFFER_SIZE)) {
            
            FastaSequenceBuilder currentSequence = null;
            String line;
            int sequenceCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (HEADER_PATTERN.matcher(line).matches()) {
                    // Process previous sequence if exists
                    if (currentSequence != null) {
                        FastaSequence sequence = currentSequence.build();
                        if (sequence.isValid()) {
                            sequenceProcessor.accept(sequence);
                            sequenceCount++;
                        }
                    }
                    
                    // Start new sequence
                    currentSequence = new FastaSequenceBuilder(line.substring(1).trim());
                    
                } else if (!line.trim().isEmpty() && currentSequence != null) {
                    // Add sequence data
                    currentSequence.appendSequence(line.trim().toUpperCase());
                }
            }
            
            // Process final sequence
            if (currentSequence != null) {
                FastaSequence sequence = currentSequence.build();
                if (sequence.isValid()) {
                    sequenceProcessor.accept(sequence);
                    sequenceCount++;
                }
            }
            
            log.info("Parsing completed: {} sequences processed from file {}", sequenceCount, file.getName());
        }
    }
    
    
    @Getter
    public static class FastaSequence {
        private final String header;
        private final String sequence;
        private final int length;
        private final boolean valid;
        
        public FastaSequence(String header, String sequence) {
            this.header = header;
            this.sequence = sequence;
            this.length = sequence.length();
            this.valid = isValidSequence(sequence);
        }
        
        public boolean isValid() { return valid && length > 0; }
        
        public double getGcContent() {
            return DNASequenceUtils.calculateGcContent(sequence);
        }
        
        public SequenceStats getStats() {
            var baseCount = countBases(sequence);
            return SequenceStats.builder()
                .aCount(baseCount.getACount())
                .tCount(baseCount.getTCount())
                .cCount(baseCount.getCCount())
                .gCount(baseCount.getGCount())
                .nCount(baseCount.getNCount())
                .totalLength(length)
                .build();
        }
        
        private boolean isValidSequence(String seq) {
            return DNASequenceUtils.isValidSequence(seq);
        }
    }
    
    @Value
    @lombok.Builder
    public static class SequenceStats {
        int aCount, tCount, cCount, gCount, nCount, totalLength;
        
        public double getGcContent() {
            return totalLength > 0 ? (double) (gCount + cCount) / totalLength * 100 : 0.0;
        }
    }
    
    private static class FastaSequenceBuilder {
        private final String header;
        private final StringBuilder sequenceBuilder;
        
        public FastaSequenceBuilder(String header) {
            this.header = header;
            this.sequenceBuilder = new StringBuilder();
        }
        
        public void appendSequence(String sequenceLine) {
            sequenceBuilder.append(sequenceLine);
        }
        
        public FastaSequence build() {
            return new FastaSequence(header, sequenceBuilder.toString());
        }
    }
}