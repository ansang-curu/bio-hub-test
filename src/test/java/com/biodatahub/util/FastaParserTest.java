package com.biodatahub.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FastaParserTest {

    private FastaParser fastaParser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fastaParser = new FastaParser();
    }

    @Test
    @DisplayName("Valid FASTA file streaming parsing")
    void testParseValidFastaFile() throws IOException {
        String fastaContent = ">seq1 description1\nATCGATCG\nGCTAGCTA\n>seq2 description2\nTTAAGGCC\n";
        Path fastaFile = tempDir.resolve("test.fasta");
        Files.write(fastaFile, fastaContent.getBytes());

        AtomicInteger sequenceCount = new AtomicInteger(0);
        
        fastaParser.parseFileStreaming(fastaFile.toFile(), sequence -> {
            sequenceCount.incrementAndGet();
            assertNotNull(sequence.getHeader());
            assertNotNull(sequence.getSequence());
            assertTrue(sequence.getLength() > 0);
            assertTrue(sequence.isValid());
        });

        assertEquals(2, sequenceCount.get());
    }

    @Test
    @DisplayName("Empty FASTA file handling")
    void testParseEmptyFastaFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.fasta");
        Files.write(emptyFile, "".getBytes());

        AtomicInteger sequenceCount = new AtomicInteger(0);
        
        fastaParser.parseFileStreaming(emptyFile.toFile(), sequence -> {
            sequenceCount.incrementAndGet();
        });

        assertEquals(0, sequenceCount.get());
    }

    @Test
    @DisplayName("FASTA sequence validation")
    void testFastaSequenceValidation() {
        FastaParser.FastaSequence validSequence = new FastaParser.FastaSequence("test", "ATCG");
        assertTrue(validSequence.isValid());
        assertEquals(4, validSequence.getLength());
        assertEquals(50.0, validSequence.getGcContent(), 0.01);

        FastaParser.FastaSequence invalidSequence = new FastaParser.FastaSequence("test", "");
        assertFalse(invalidSequence.isValid());
        assertEquals(0, invalidSequence.getLength());
    }

    @Test
    @DisplayName("Sequence statistics calculation")
    void testSequenceStats() {
        FastaParser.FastaSequence sequence = new FastaParser.FastaSequence("test", "ATCGATCG");
        FastaParser.SequenceStats stats = sequence.getStats();
        
        assertEquals(2, stats.getACount());
        assertEquals(2, stats.getTCount());
        assertEquals(2, stats.getCCount());
        assertEquals(2, stats.getGCount());
        assertEquals(0, stats.getNCount());
        assertEquals(8, stats.getTotalLength());
        assertEquals(50.0, stats.getGcContent(), 0.01);
    }
}