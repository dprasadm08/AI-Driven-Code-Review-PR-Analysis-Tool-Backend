package com.aiprreview.dto.analysis;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualPrAnalysisRequest {

    @NotBlank(message = "pullRequestId is required")
    @Size(min = 1, max = 255, message = "pullRequestId must be between 1 and 255 characters")
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
    @Size(max = 255, message = "GitHub token must not exceed 255 characters")
    private String githubToken;

    /**
     * Whether to fetch and include the full unified diff.
     */
    private boolean includeDiff = false;

    /**
     * Analysis modules to run. Null / empty means run ALL five modules.
     * Valid values: bug, security, performance, code_quality, test_case
     */
    @Size(max = 5, message = "Cannot request more than 5 analysis modules")
    private List<
            @Pattern(regexp = "^(bug|security|performance|code_quality|test_case)$",
                     message = "Each module must be one of: bug, security, performance, code_quality, test_case")
            String> modules;
}
