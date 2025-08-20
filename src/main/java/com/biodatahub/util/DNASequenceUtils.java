package com.biodatahub.util;

import lombok.*;

/**
 * DNA 서열 분석 유틸리티 클래스
 */
public class DNASequenceUtils {
    
    /**
     * DNA 서열의 염기를 계산합니다
     */
    public static BaseCount countBases(String sequence) {
        if (sequence == null || sequence.isEmpty()) {
            return new BaseCount(0, 0, 0, 0, 0);
        }
        
        int a = 0, t = 0, c = 0, g = 0, n = 0;
        
        for (char base : sequence.toCharArray()) {
            switch (base) {
                case 'A': a++; break;
                case 'T': t++; break;
                case 'C': c++; break;
                case 'G': g++; break;
                case 'N': n++; break;
            }
        }
        
        return new BaseCount(a, t, c, g, n);
    }
    
    /**
     * DNA 서열의 유효성을 검증합니다
     */
    public static boolean isValidSequence(String sequence) {
        if (sequence == null || sequence.isEmpty()) {
            return false;
        }
        return sequence.matches("^[ATCGN]+$");
    }
    
    /**
     * GC 함량을 계산합니다
     */
    public static double calculateGcContent(String sequence) {
        if (sequence == null || sequence.isEmpty()) {
            return 0.0;
        }
        
        int gcCount = 0;
        for (char c : sequence.toCharArray()) {
            if (c == 'G' || c == 'C') gcCount++;
        }
        
        return (double) gcCount / sequence.length() * 100;
    }
    
    /**
     * 염기 개수 결과를 담는 Value Object
     */
    @Value
    public static class BaseCount {
        int aCount, tCount, cCount, gCount, nCount;
        
        public int getTotalLength() {
            return aCount + tCount + cCount + gCount + nCount;
        }
        
        public double getGcContent() {
            int total = getTotalLength();
            return total > 0 ? (double) (gCount + cCount) / total * 100 : 0.0;
        }
    }
}