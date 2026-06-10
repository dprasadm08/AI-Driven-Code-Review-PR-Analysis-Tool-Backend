package com.aiprreview.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestCaseAnalysisResultParser {

    private final ObjectMapper objectMapper;

    public TestCaseAnalysisResult parse(String rawContent, String provider) {
        if (rawContent == null || rawContent.isBlank()) {
            return fallback("AI returned empty content", rawContent, provider);
        }

        String json = extractJson(rawContent);

        try {
            JsonNode root = objectMapper.readTree(json);
            List<TestCaseFinding> findings = parseFindings(root.path("testFindings"));

            return TestCaseAnalysisResult.builder()
                    .summary(textOrEmpty(root, "summary"))
                    .missingTestCount(root.path("missingTestCount").asInt(findings.size()))
                    .coverageRiskLevel(textOrDefault(root, "coverageRiskLevel", "unknown"))
                    .testFindings(findings)
                    .confidence(root.path("confidence").asDouble(0.0))
                    .analysisNotes(textOrEmpty(root, "analysisNotes"))
                    .rawContent(rawContent)
                    .provider(provider)
                    .build();

        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse test-case analysis JSON — using fallback. error={}", ex.getMessage());
            return fallback(rawContent, rawContent, provider);
        }
    }

    private List<TestCaseFinding> parseFindings(JsonNode findingsNode) {
        List<TestCaseFinding> findings = new ArrayList<>();
        if (findingsNode == null || !findingsNode.isArray()) {
            return findings;
        }

        for (JsonNode node : findingsNode) {
            findings.add(TestCaseFinding.builder()
                    .id(textOrEmpty(node, "id"))
                    .severity(textOrDefault(node, "severity", "medium"))
                    .category(textOrDefault(node, "category", "other"))
                    .title(textOrEmpty(node, "title"))
                    .description(textOrEmpty(node, "description"))
                    .file(textOrEmpty(node, "file"))
                    .startLine(intOrNull(node, "startLine"))
                    .endLine(intOrNull(node, "endLine"))
                    .targetCode(textOrEmpty(node, "targetCode"))
                    .suggestedTest(textOrEmpty(node, "suggestedTest"))
                    .build());
        }
        return findings;
    }

    private String extractJson(String raw) {
        if (raw.contains("```")) {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return raw.substring(start, end + 1);
            }
        }
        return raw.trim();
    }

    private TestCaseAnalysisResult fallback(String summary, String rawContent, String provider) {
        return TestCaseAnalysisResult.builder()
                .summary(summary)
                .missingTestCount(0)
                .coverageRiskLevel("unknown")
                .testFindings(new ArrayList<>())
                .confidence(0.0)
                .rawContent(rawContent)
                .provider(provider)
                .build();
    }

    private String textOrEmpty(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return n.isMissingNode() || n.isNull() ? "" : n.asText();
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode n = node.path(field);
        return (n.isMissingNode() || n.isNull() || n.asText().isBlank()) ? defaultValue : n.asText();
    }

    private Integer intOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isMissingNode() || n.isNull()) ? null : n.asInt();
    }
}
