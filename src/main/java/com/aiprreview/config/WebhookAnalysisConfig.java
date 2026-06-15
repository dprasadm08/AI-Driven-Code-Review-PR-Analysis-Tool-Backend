package com.aiprreview.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "app.github.webhook.analysis")
public class WebhookAnalysisConfig {

    /**
     * Enable automatic analysis triggering on webhook events
     */
    private boolean enabled = true;

    /**
     * AI provider to use for automatic analysis (openai, claude)
     */
    private String provider = "openai";

    /**
     * Include full diff in automatic analysis requests
     */
    private boolean includeDiff = false;

    /**
     * PR event actions that should trigger automatic analysis
     * Default: opened, synchronize
     */
    private String triggerActions = "opened,synchronize";

    /**
     * Parsed set of trigger actions
     */
    public Set<String> getTriggerActionsSet() {
        if (triggerActions == null || triggerActions.isBlank()) {
            return Set.of();
        }
        return Set.of(triggerActions.split(","))
                .stream()
                .map(String::trim)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Check if an action should trigger analysis
     */
    public boolean shouldTriggerAnalysis(String action) {
        if (!enabled || action == null || action.isBlank()) {
            return false;
        }
        return getTriggerActionsSet().contains(action.trim().toLowerCase());
    }
}
