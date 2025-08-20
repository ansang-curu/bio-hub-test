package com.biodatahub.model;

import lombok.*;

import java.time.LocalDateTime;

import static com.biodatahub.util.DNASequenceUtils.countBases;
import com.biodatahub.util.DNASequenceUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequenceData {
    
    private Long id;
    private String fileId;
    private String sequenceId;
    private String header;
    private String sequence;
    private Integer sequenceLength;
    private Double gcContent;
    private Integer aCount;
    private Integer tCount;
    private Integer cCount;
    private Integer gCount;
    private Integer nCount;
    private Boolean isValid;
    private LocalDateTime createdAt;
    
    public double calculateGcContent() {
        return DNASequenceUtils.calculateGcContent(sequence);
    }
    
    public boolean isValidSequence() {
        return DNASequenceUtils.isValidSequence(sequence);
    }
    
    public void calculateBaseCounts() {
        DNASequenceUtils.BaseCount baseCount = countBases(sequence);
        
        this.aCount = baseCount.getACount();
        this.tCount = baseCount.getTCount();
        this.cCount = baseCount.getCCount();
        this.gCount = baseCount.getGCount();
        this.nCount = baseCount.getNCount();
        this.sequenceLength = sequence != null ? sequence.length() : 0;
        this.gcContent = calculateGcContent();
        this.isValid = isValidSequence();
    }
    
    public String getComposition() {
        if (sequenceLength == null || sequenceLength == 0) {
            return "A:0%, T:0%, C:0%, G:0%, N:0%";
        }
        
        double aPercent = (aCount != null ? aCount : 0) * 100.0 / sequenceLength;
        double tPercent = (tCount != null ? tCount : 0) * 100.0 / sequenceLength;
        double cPercent = (cCount != null ? cCount : 0) * 100.0 / sequenceLength;
        double gPercent = (gCount != null ? gCount : 0) * 100.0 / sequenceLength;
        double nPercent = (nCount != null ? nCount : 0) * 100.0 / sequenceLength;
        
        return String.format("A:%.1f%%, T:%.1f%%, C:%.1f%%, G:%.1f%%, N:%.1f%%", 
                           aPercent, tPercent, cPercent, gPercent, nPercent);
    }
}