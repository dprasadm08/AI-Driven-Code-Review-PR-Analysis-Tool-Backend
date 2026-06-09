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
public class CodeQualityAnalysisResultParser {

    private final ObjectMapper objectMapper;

    public CodeQualityAnalysisResult parse(String rawContent, String provider) {
        if (rawContent == null || rawContent.isBlank()) {
            return fallback("AI returned empty content", rawContent, provider);
        }

        String json = extractJson(rawContent);

        try {
            JsonNode root = objectMapper.readTree(json);
            List<CodeSmellFinding> smells = parseSmells(root.path("smells"));

            return CodeQualityAnalysisResult.builder()
                    .summary(textOrEmpty(root, "summary"))
                    .smellCount(root.path("smellCount").asInt(smells.size()))
                    .qualityScore(root.path("qualityScore").isMissingNode() || root.path("qualityScore").isNull()
                            ? null : root.path("qualityScore").asInt())
                    .riskLevel(textOrDefault(root, "riskLevel", "unknown"))
                    .smells(smells)
                    .confidence(root.path("confidence").asDouble(0.0))
                    .analysisNotes(textOrEmpty(root, "analysisNotes"))
                    .rawContent(rawContent)
                    .provider(provider)
                    .build();

        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse code-quality analysis JSON — using fallback. error={}", ex.getMessage());
            return fallback(rawContent, rawContent, provider);
        }
    }

    private List<CodeSmellFinding> parseSmells(JsonNode smellsNode) {
        List<CodeSmellFinding> smells = new ArrayList<>();
        if (smellsNode == null || !smellsNode.isArray()) {
            return smells;
        }

        for (JsonNode node : smellsNode) {
            smells.add(CodeSmellFinding.builder()
                    .id(textOrEmpty(node, "id"))
                    .severity(textOrDefault(node, "severity", "medium"))
                    .category(textOrDefault(node, "category", "other"))
                    .title(textOrEmpty(node, "title"))
                    .description(textOrEmpty(node, "description"))
                    .file(textOrEmpty(node, "file"))
                    .startLine(intOrNull(node, "startLine"))
                    .endLine(intOrNull(node, "endLine"))
                    .codeSnippet(textOrEmpty(node, "codeSnippet"))
                    .recommendation(textOrEmpty(node, "recommendation"))
                    .build());
        }
        return smells;
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

    private CodeQualityAnalysisResult fallback(String summary, String rawContent, String provider) {
        return CodeQualityAnalysisResult.builder()
                .summary(summary)
                .smellCount(0)
                .qualityScore(null)
                .riskLevel("unknown")
                .smells(new ArrayList<>())
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
