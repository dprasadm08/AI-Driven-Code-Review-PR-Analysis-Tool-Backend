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
public class SecurityAnalysisResultParser {

    private final ObjectMapper objectMapper;

    public SecurityAnalysisResult parse(String rawContent, String provider) {
        if (rawContent == null || rawContent.isBlank()) {
            return fallback("AI returned empty content", rawContent, provider);
        }

        String json = extractJson(rawContent);

        try {
            JsonNode root = objectMapper.readTree(json);
            List<SecurityVulnerability> vulnerabilities = parseVulnerabilities(root.path("vulnerabilities"));

            return SecurityAnalysisResult.builder()
                    .summary(textOrEmpty(root, "summary"))
                    .vulnerabilityCount(root.path("vulnerabilityCount").asInt(vulnerabilities.size()))
                    .riskLevel(textOrDefault(root, "riskLevel", "unknown"))
                    .vulnerabilities(vulnerabilities)
                    .confidence(root.path("confidence").asDouble(0.0))
                    .analysisNotes(textOrEmpty(root, "analysisNotes"))
                    .rawContent(rawContent)
                    .provider(provider)
                    .build();

        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse security analysis JSON — using fallback. error={}", ex.getMessage());
            return fallback(rawContent, rawContent, provider);
        }
    }

    private List<SecurityVulnerability> parseVulnerabilities(JsonNode vulnerabilitiesNode) {
        List<SecurityVulnerability> vulnerabilities = new ArrayList<>();
        if (vulnerabilitiesNode == null || !vulnerabilitiesNode.isArray()) {
            return vulnerabilities;
        }

        for (JsonNode node : vulnerabilitiesNode) {
            SecurityVulnerability vulnerability = SecurityVulnerability.builder()
                    .id(textOrEmpty(node, "id"))
                    .severity(textOrDefault(node, "severity", "medium"))
                    .category(textOrDefault(node, "category", "other"))
                    .title(textOrEmpty(node, "title"))
                    .description(textOrEmpty(node, "description"))
                    .file(textOrEmpty(node, "file"))
                    .startLine(intOrNull(node, "startLine"))
                    .endLine(intOrNull(node, "endLine"))
                    .vulnerableCode(textOrEmpty(node, "vulnerableCode"))
                    .exploitScenario(textOrEmpty(node, "exploitScenario"))
                    .recommendation(textOrEmpty(node, "recommendation"))
                    .build();
            vulnerabilities.add(vulnerability);
        }

        return vulnerabilities;
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

    private SecurityAnalysisResult fallback(String summary, String rawContent, String provider) {
        return SecurityAnalysisResult.builder()
                .summary(summary)
                .vulnerabilityCount(0)
                .riskLevel("unknown")
                .vulnerabilities(new ArrayList<>())
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
