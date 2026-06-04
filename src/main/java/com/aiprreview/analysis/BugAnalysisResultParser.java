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
public class BugAnalysisResultParser {

    private final ObjectMapper objectMapper;

    public BugAnalysisResult parse(String rawContent, String provider) {
        if (rawContent == null || rawContent.isBlank()) {
            return fallback("AI returned empty content", rawContent, provider);
        }

        String json = extractJson(rawContent);

        try {
            JsonNode root = objectMapper.readTree(json);

            List<BugFinding> bugs = parseFindings(root.path("bugs"));

            return BugAnalysisResult.builder()
                    .summary(textOrEmpty(root, "summary"))
                    .bugCount(root.path("bugCount").asInt(bugs.size()))
                    .riskLevel(textOrDefault(root, "riskLevel", "unknown"))
                    .bugs(bugs)
                    .confidence(root.path("confidence").asDouble(0.0))
                    .analysisNotes(textOrEmpty(root, "analysisNotes"))
                    .rawContent(rawContent)
                    .provider(provider)
                    .build();

        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse bug analysis JSON — returning raw summary. error={}", ex.getMessage());
            return fallback(rawContent, rawContent, provider);
        }
    }

    private List<BugFinding> parseFindings(JsonNode bugsNode) {
        List<BugFinding> findings = new ArrayList<>();
        if (bugsNode == null || !bugsNode.isArray()) {
            return findings;
        }

        for (JsonNode node : bugsNode) {
            BugFinding finding = BugFinding.builder()
                    .id(textOrEmpty(node, "id"))
                    .severity(textOrDefault(node, "severity", "medium"))
                    .type(textOrDefault(node, "type", "other"))
                    .title(textOrEmpty(node, "title"))
                    .description(textOrEmpty(node, "description"))
                    .file(textOrEmpty(node, "file"))
                    .startLine(intOrNull(node, "startLine"))
                    .endLine(intOrNull(node, "endLine"))
                    .buggyCode(textOrEmpty(node, "buggyCode"))
                    .recommendation(textOrEmpty(node, "recommendation"))
                    .testScenario(textOrEmpty(node, "testScenario"))
                    .build();
            findings.add(finding);
        }
        return findings;
    }

    private String extractJson(String raw) {
        // Strip markdown code fences if present
        if (raw.contains("```")) {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return raw.substring(start, end + 1);
            }
        }
        return raw.trim();
    }

    private BugAnalysisResult fallback(String summary, String rawContent, String provider) {
        return BugAnalysisResult.builder()
                .summary(summary)
                .bugCount(0)
                .riskLevel("unknown")
                .bugs(new ArrayList<>())
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
