package com.aiprreview.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnalysisResultParsersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bugParser_ShouldParseJsonInsideCodeFence_AndApplyDefaults() {
        BugAnalysisResultParser parser = new BugAnalysisResultParser(objectMapper);
        String raw = """
                ```json
                {
                  "summary": "Bug summary",
                  "bugs": [
                    {
                      "id": "BUG-1",
                      "severity": "",
                      "type": null,
                      "title": "Null pointer",
                      "file": "src/A.java",
                      "startLine": 10
                    }
                  ],
                  "confidence": 0.81
                }
                ```
                """;

        BugAnalysisResult result = parser.parse(raw, "openai");

        assertEquals("Bug summary", result.getSummary());
        assertEquals(1, result.getBugCount());
        assertEquals("unknown", result.getRiskLevel());
        assertEquals(0.81, result.getConfidence());
        assertEquals("openai", result.getProvider());
        assertEquals(1, result.getBugs().size());
        assertEquals("medium", result.getBugs().get(0).getSeverity());
        assertEquals("other", result.getBugs().get(0).getType());
        assertNull(result.getBugs().get(0).getEndLine());
    }

    @Test
    void securityParser_ShouldFallback_WhenInvalidJson() {
        SecurityAnalysisResultParser parser = new SecurityAnalysisResultParser(objectMapper);
        String raw = "not-json";

        SecurityAnalysisResult result = parser.parse(raw, "claude");

        assertEquals("not-json", result.getSummary());
        assertEquals(0, result.getVulnerabilityCount());
        assertEquals("unknown", result.getRiskLevel());
        assertEquals(0, result.getVulnerabilities().size());
        assertEquals("claude", result.getProvider());
    }

    @Test
    void performanceParser_ShouldUseIssueArraySize_WhenIssueCountMissing() {
        PerformanceAnalysisResultParser parser = new PerformanceAnalysisResultParser(objectMapper);
        String raw = """
                {
                  "summary": "Perf summary",
                  "issues": [
                    {"id":"P1","title":"Slow query"},
                    {"id":"P2","title":"N+1 call"}
                  ]
                }
                """;

        PerformanceAnalysisResult result = parser.parse(raw, "openai");

        assertEquals("Perf summary", result.getSummary());
        assertEquals(2, result.getIssueCount());
        assertEquals("unknown", result.getRiskLevel());
        assertEquals(2, result.getIssues().size());
    }

    @Test
    void codeQualityParser_ShouldParseQualityScore_WhenProvided() {
        CodeQualityAnalysisResultParser parser = new CodeQualityAnalysisResultParser(objectMapper);
        String raw = """
                {
                  "summary": "Quality summary",
                  "smellCount": 1,
                  "qualityScore": 78,
                  "riskLevel": "low",
                  "smells": [
                    {"id":"S1","title":"Long method","severity":"high","category":"maintainability"}
                  ]
                }
                """;

        CodeQualityAnalysisResult result = parser.parse(raw, "claude");

        assertEquals("Quality summary", result.getSummary());
        assertEquals(1, result.getSmellCount());
        assertEquals(78, result.getQualityScore());
        assertEquals("low", result.getRiskLevel());
        assertEquals(1, result.getSmells().size());
    }

    @Test
    void testCaseParser_ShouldFallback_OnEmptyInput() {
        TestCaseAnalysisResultParser parser = new TestCaseAnalysisResultParser(objectMapper);

        TestCaseAnalysisResult result = parser.parse("   ", "openai");

        assertEquals("AI returned empty content", result.getSummary());
        assertEquals(0, result.getMissingTestCount());
        assertEquals("unknown", result.getCoverageRiskLevel());
        assertEquals(0, result.getTestFindings().size());
        assertEquals("openai", result.getProvider());
    }
}
