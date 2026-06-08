package com.aiprreview.analysis;

import com.aiprreview.ai.AiProvider;
import com.aiprreview.ai.AiProviderRouter;
import com.aiprreview.dto.pullrequest.PullRequestFileResponse;
import com.aiprreview.dto.pullrequest.PullRequestWithFilesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceAnalysisService {

    @Value("classpath:prompts/performance-analysis.txt")
    private Resource performancePromptTemplate;

    private final AiProviderRouter aiProviderRouter;
    private final PerformanceAnalysisResultParser resultParser;

    public PerformanceAnalysisResult analyze(PullRequestWithFilesResponse pr, String preferredProvider) {
        AiProvider provider = aiProviderRouter.resolveProvider(preferredProvider);
        log.info("Running performance analysis on PR #{} repo={} provider={}",
                pr.getPrNumber(), pr.getRepositoryFullName(), provider.getProviderName());

        String prompt = buildPrompt(pr);
        AiProvider.AiProviderResponse response = provider.analyze(prompt);

        log.info("Performance analysis AI response received provider={} contentLength={}",
                response.provider(), response.content() != null ? response.content().length() : 0);

        PerformanceAnalysisResult result = resultParser.parse(response.content(), response.provider());
        log.info("Performance analysis parsed: issueCount={} riskLevel={} confidence={}",
                result.getIssueCount(), result.getRiskLevel(), result.getConfidence());

        return result;
    }

    String buildPrompt(PullRequestWithFilesResponse pr) {
        String template = loadTemplate();
        String filesSummary = buildFilesSummary(pr.getFiles());
        String diffInput = buildDiff(pr);

        return template
                .replace("{{repositoryFullName}}", safe(pr.getRepositoryFullName()))
                .replace("{{prNumber}}", String.valueOf(pr.getPrNumber()))
                .replace("{{title}}", safe(pr.getTitle()))
                .replace("{{baseBranch}}", safe(pr.getBaseBranch()))
                .replace("{{headBranch}}", safe(pr.getHeadBranch()))
                .replace("{{author}}", safe(pr.getAuthor()))
                .replace("{{description}}", safe(pr.getDescription()))
                .replace("{{filesSummary}}", filesSummary)
                .replace("{{diffInput}}", diffInput);
    }

    private String buildFilesSummary(List<PullRequestFileResponse> files) {
        if (files == null || files.isEmpty()) return "No changed files metadata available.";
        StringBuilder sb = new StringBuilder();
        for (PullRequestFileResponse f : files) {
            sb.append("- ").append(safe(f.getFilename()))
                    .append(" [").append(safe(f.getStatus())).append("]")
                    .append(" +").append(Objects.requireNonNullElse(f.getAdditions(), 0))
                    .append(" -").append(Objects.requireNonNullElse(f.getDeletions(), 0))
                    .append('\n');
        }
        return sb.toString();
    }

    private String buildDiff(PullRequestWithFilesResponse pr) {
        if (pr.getDiff() != null && !pr.getDiff().isBlank()) {
            return pr.getDiff();
        }
        if (pr.getFiles() == null || pr.getFiles().isEmpty()) {
            return "No diff content available.";
        }
        StringBuilder sb = new StringBuilder();
        for (PullRequestFileResponse f : pr.getFiles()) {
            sb.append("diff --git a/").append(safe(f.getFilename())).append(" b/").append(safe(f.getFilename())).append('\n');
            sb.append("status: ").append(safe(f.getStatus()))
                    .append(" (+").append(Objects.requireNonNullElse(f.getAdditions(), 0))
                    .append(" -").append(Objects.requireNonNullElse(f.getDeletions(), 0)).append(")\n");
            if (f.getPatch() != null && !f.getPatch().isBlank()) {
                sb.append(f.getPatch()).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String loadTemplate() {
        try {
            return new String(performancePromptTemplate.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("Could not load performance-analysis.txt, using minimal fallback. reason={}", ex.getMessage());
            return "Analyze the following PR diff for performance issues and return JSON with fields: " +
                    "summary, issueCount, riskLevel, issues[], confidence.\n\nDiff:\n{{diffInput}}";
        }
    }

    private String safe(String v) {
        return v != null ? v : "";
    }
}
