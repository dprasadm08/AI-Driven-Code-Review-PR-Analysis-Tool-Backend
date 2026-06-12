package com.aiprreview.dto.analysis;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualPrAnalysisRequest {

    @NotBlank(message = "pullRequestId is required")
    private String pullRequestId;

    /**
     * Optional AI provider override: openai | claude.
     * When omitted the configured default is used.
     */
    @Pattern(regexp = "^(openai|claude)?$", message = "provider must be 'openai' or 'claude'")
    private String provider;

    /**
     * Optional GitHub personal access token.
     * Falls back to the token stored on the authenticated user account.
     */
    private String githubToken;

    /**
     * Whether to fetch and include the full unified diff.
     */
    private boolean includeDiff = false;

    /**
     * Analysis modules to run. Null / empty means run ALL five modules.
     * Valid values: bug, security, performance, code_quality, test_case
     */
    private java.util.List<
            @Pattern(regexp = "^(bug|security|performance|code_quality|test_case)$",
                     message = "Each module must be one of: bug, security, performance, code_quality, test_case")
            String> modules;
}
