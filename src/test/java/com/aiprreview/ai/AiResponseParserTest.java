package com.aiprreview.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiResponseParserTest {

    @Test
    void extractSummary_ShouldReturnEmpty_WhenInputIsNull() {
        assertEquals("", AiResponseParser.extractSummary(null, 20));
    }

    @Test
    void extractSummary_ShouldReturnEmpty_WhenInputIsBlank() {
        assertEquals("", AiResponseParser.extractSummary("   ", 20));
    }

    @Test
    void extractSummary_ShouldReturnOriginal_WhenWithinLimit() {
        assertEquals("short text", AiResponseParser.extractSummary("short text", 20));
    }

    @Test
    void extractSummary_ShouldTruncateAndAppendEllipsis_WhenExceedsLimit() {
        assertEquals("abcdef...", AiResponseParser.extractSummary("abcdefghi", 6));
    }
}
