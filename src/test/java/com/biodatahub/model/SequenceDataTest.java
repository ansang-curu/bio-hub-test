package com.biodatahub.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class SequenceDataTest {

    @Test
    @DisplayName("GC content calculation")
    void testCalculateGcContent() {
        SequenceData sequence = SequenceData.builder()
                .sequence("ATCGATCG")
                .gCount(2)
                .cCount(2)
                .sequenceLength(8)
                .build();

        double gcContent = sequence.calculateGcContent();
        assertEquals(50.0, gcContent, 0.01);
    }

    @Test
    @DisplayName("GC content with empty sequence")
    void testCalculateGcContentWithEmptySequence() {
        SequenceData sequence = SequenceData.builder()
                .sequence("")
                .sequenceLength(0)
                .build();

        double gcContent = sequence.calculateGcContent();
        assertEquals(0.0, gcContent, 0.01);
    }

    @Test
    @DisplayName("Base counts calculation")
    void testCalculateBaseCounts() {
        SequenceData sequence = SequenceData.builder()
                .sequence("ATCGATCG")
                .build();

        sequence.calculateBaseCounts();
        
        assertEquals(2, sequence.getACount());
        assertEquals(2, sequence.getTCount());
        assertEquals(2, sequence.getCCount());
        assertEquals(2, sequence.getGCount());
        assertEquals(0, sequence.getNCount());
        assertEquals(8, sequence.getSequenceLength());
    }

    @Test
    @DisplayName("Base counts with N characters")
    void testCalculateBaseCountsWithN() {
        SequenceData sequence = SequenceData.builder()
                .sequence("ATCGNNNN")
                .build();

        sequence.calculateBaseCounts();
        
        assertEquals(1, sequence.getACount());
        assertEquals(1, sequence.getTCount());
        assertEquals(1, sequence.getCCount());
        assertEquals(1, sequence.getGCount());
        assertEquals(4, sequence.getNCount());
        assertEquals(8, sequence.getSequenceLength());
    }

    @Test
    @DisplayName("Empty sequence handling")
    void testEmptySequence() {
        SequenceData sequence = SequenceData.builder()
                .sequence("")
                .build();

        sequence.calculateBaseCounts();
        
        assertEquals(0, sequence.getACount());
        assertEquals(0, sequence.getTCount());
        assertEquals(0, sequence.getCCount());
        assertEquals(0, sequence.getGCount());
        assertEquals(0, sequence.getNCount());
        assertEquals(0.0, sequence.calculateGcContent());
    }

    @Test
    @DisplayName("Null sequence handling")
    void testNullSequence() {
        SequenceData sequence = SequenceData.builder()
                .sequence(null)
                .build();

        sequence.calculateBaseCounts();
        
        assertEquals(0, sequence.getACount());
        assertEquals(0, sequence.getTCount());
        assertEquals(0, sequence.getCCount());
        assertEquals(0, sequence.getGCount());
        assertEquals(0, sequence.getNCount());
        assertEquals(0.0, sequence.calculateGcContent());
    }

    @Test
    @DisplayName("Invalid characters in sequence")
    void testInvalidCharacters() {
        SequenceData sequence = SequenceData.builder()
                .sequence("ATCGXYZ123")
                .build();

        sequence.calculateBaseCounts();
        
        // Should only count valid DNA bases
        assertEquals(1, sequence.getACount());
        assertEquals(1, sequence.getTCount());
        assertEquals(1, sequence.getCCount());
        assertEquals(1, sequence.getGCount());
        assertEquals(0, sequence.getNCount());
    }

    @Test
    @DisplayName("Valid sequence check")
    void testIsValidSequence() {
        SequenceData validSequence = SequenceData.builder()
                .sequence("ATCGNATCG")
                .build();
        
        assertTrue(validSequence.isValidSequence());

        SequenceData invalidSequence = SequenceData.builder()
                .sequence("ATCGXYZ")
                .build();
        
        assertFalse(invalidSequence.isValidSequence());
        
        SequenceData emptySequence = SequenceData.builder()
                .sequence("")
                .build();
        
        assertFalse(emptySequence.isValidSequence());
    }

    @Test
    @DisplayName("Composition string formatting")
    void testGetComposition() {
        SequenceData sequence = SequenceData.builder()
                .sequence("ATCGATCG")
                .build();
        
        sequence.calculateBaseCounts();
        
        String composition = sequence.getComposition();
        assertEquals("A:25.0%, T:25.0%, C:25.0%, G:25.0%, N:0.0%", composition);
    }

    @Test
    @DisplayName("Builder pattern validation")
    void testBuilderPattern() {
        SequenceData sequence = SequenceData.builder()
                .sequenceId("test123")
                .header("Test sequence")
                .sequence("ATCG")
                .sequenceLength(4)
                .fileId("file123")
                .build();

        assertEquals("test123", sequence.getSequenceId());
        assertEquals("Test sequence", sequence.getHeader());
        assertEquals("ATCG", sequence.getSequence());
        assertEquals(4, sequence.getSequenceLength());
        assertEquals("file123", sequence.getFileId());
    }
}