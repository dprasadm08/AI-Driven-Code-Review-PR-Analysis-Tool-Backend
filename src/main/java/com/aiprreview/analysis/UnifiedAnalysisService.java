package com.aiprreview.analysis;

import com.aiprreview.dto.analysis.UnifiedAnalysisResponse;
import com.aiprreview.dto.analysis.UnifiedFinding;
import com.aiprreview.dto.pullrequest.PullRequestWithFilesResponse;
import com.aiprreview.entity.AnalysisResult;
import com.aiprreview.entity.PullRequest;
import com.aiprreview.exception.ResourceNotFoundException;
import com.aiprreview.repository.AnalysisResultRepository;
import com.aiprreview.repository.PullRequestRepository;
import com.aiprreview.service.AuthService;
import com.aiprreview.service.PullRequestService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedAnalysisService {

    private final PullRequestService pullRequestService;
    private final PullRequestRepository pullRequestRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AuthService authService;
    private final BugAnalysisService bugAnalysisService;
    private final SecurityAnalysisService securityAnalysisService;
    private final PerformanceAnalysisService performanceAnalysisService;
    private final CodeQualityAnalysisService codeQualityAnalysisService;
    private final TestCaseAnalysisService testCaseAnalysisService;
    private final ObjectMapper objectMapper;

    public UnifiedAnalysisResponse analyzeAndStore(
            String pullRequestId,
            String preferredProvider,
            String githubToken,
            boolean includeDiff) {

        PullRequest pullRequest = pullRequestRepository.findById(pullRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pull request not found with id: " + pullRequestId));

        // Validate ownership when an authenticated user is present (skip for system/webhook async calls)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String currentUserId = authService.getCurrentUser().getId();
            if (!pullRequest.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("You do not have access to this pull request");
            }
        }

        pullRequest.setAnalysisStatus("in_progress");
        pullRequest.setUpdatedAt(LocalDateTime.now());
        pullRequestRepository.save(pullRequest);

        try {
            PullRequestWithFilesResponse pr = pullRequestService.getPullRequestWithFiles(
                    pullRequestId,
                    githubToken,
                    includeDiff
            );

            BugAnalysisResult bug = bugAnalysisService.analyze(pr, preferredProvider);
            SecurityAnalysisResult security = securityAnalysisService.analyze(pr, preferredProvider);
            PerformanceAnalysisResult performance = performanceAnalysisService.analyze(pr, preferredProvider);
            CodeQualityAnalysisResult codeQuality = codeQualityAnalysisService.analyze(pr, preferredProvider);
            TestCaseAnalysisResult testCase = testCaseAnalysisService.analyze(pr, preferredProvider);

            List<UnifiedFinding> allFindings = new ArrayList<>();
            Map<String, Integer> countsBySeverity = new LinkedHashMap<>();
            Map<String, Integer> countsByModule = new LinkedHashMap<>();

            int bugCount = Optional.ofNullable(bug.getBugs()).map(List::size).orElse(0);
            int securityCount = Optional.ofNullable(security.getVulnerabilities()).map(List::size).orElse(0);
            int perfCount = Optional.ofNullable(performance.getIssues()).map(List::size).orElse(0);
            int qualityCount = Optional.ofNullable(codeQuality.getSmells()).map(List::size).orElse(0);
            int testCount = Optional.ofNullable(testCase.getTestFindings()).map(List::size).orElse(0);

            countsByModule.put("bug", bugCount);
            countsByModule.put("security", securityCount);
            countsByModule.put("performance", perfCount);
            countsByModule.put("code_quality", qualityCount);
            countsByModule.put("test_case", testCount);

            addBugFindings(bug, allFindings, countsBySeverity);
            addSecurityFindings(security, allFindings, countsBySeverity);
            addPerformanceFindings(performance, allFindings, countsBySeverity);
            addCodeQualityFindings(codeQuality, allFindings, countsBySeverity);
            addTestCaseFindings(testCase, allFindings, countsBySeverity);

            int totalFindings = allFindings.size();
            double overallConfidence = average(
                    bug.getConfidence(),
                    security.getConfidence(),
                    performance.getConfidence(),
                    codeQuality.getConfidence(),
                    testCase.getConfidence()
            );

            String provider = firstNonBlank(
                    bug.getProvider(),
                    security.getProvider(),
                    performance.getProvider(),
                    codeQuality.getProvider(),
                    testCase.getProvider(),
                    preferredProvider,
                    "unknown"
            );

            String overallRiskLevel = maxRiskLevel(
                    bug.getRiskLevel(),
                    security.getRiskLevel(),
                    performance.getRiskLevel(),
                    codeQuality.getRiskLevel(),
                    testCase.getCoverageRiskLevel()
            );

            String overallSummary = buildSummary(bug, security, performance, codeQuality, testCase, totalFindings);

            Map<String, Object> findings = new LinkedHashMap<>();
            findings.put("bug", toMap(bug));
            findings.put("security", toMap(security));
            findings.put("performance", toMap(performance));
            findings.put("codeQuality", toMap(codeQuality));
            findings.put("testCase", toMap(testCase));
            findings.put("allFindings", allFindings);
            findings.put("countsByModule", countsByModule);
            findings.put("countsBySeverity", countsBySeverity);
            findings.put("totalFindings", totalFindings);
            findings.put("overallRiskLevel", overallRiskLevel);
            findings.put("overallConfidence", overallConfidence);

            Map<String, Object> rawOutput = new HashMap<>();
            rawOutput.put("bug", bug.getRawContent());
            rawOutput.put("security", security.getRawContent());
            rawOutput.put("performance", performance.getRawContent());
            rawOutput.put("codeQuality", codeQuality.getRawContent());
            rawOutput.put("testCase", testCase.getRawContent());
            rawOutput.put("provider", provider);

            LocalDateTime now = LocalDateTime.now();
            AnalysisResult saved = analysisResultRepository.save(AnalysisResult.builder()
                    .pullRequestId(pullRequestId)
                    .repositoryId(pullRequest.getRepositoryId())
                    .userId(pullRequest.getUserId())
                    .status("completed")
                    .summary(overallSummary)
                    .findings(findings)
                    .score(overallConfidence)
                    .model(provider)
                    .createdAt(now)
                    .completedAt(now)
                    .rawOutput(rawOutput)
                    .build());

            pullRequest.setAnalysisStatus("completed");
            pullRequest.setAnalysisResultId(saved.getId());
            pullRequest.setAnalyzedAt(now);
            pullRequest.setUpdatedAt(now);
            pullRequestRepository.save(pullRequest);

            return UnifiedAnalysisResponse.builder()
                    .analysisResultId(saved.getId())
                    .pullRequestId(pullRequestId)
                    .repositoryId(pullRequest.getRepositoryId())
                    .userId(pullRequest.getUserId())
                    .provider(provider)
                    .status("completed")
                    .overallSummary(overallSummary)
                    .overallRiskLevel(overallRiskLevel)
                    .totalFindings(totalFindings)
                    .overallConfidence(overallConfidence)
                    .countsByModule(countsByModule)
                    .countsBySeverity(countsBySeverity)
                    .bugAnalysis(bug)
                    .securityAnalysis(security)
                    .performanceAnalysis(performance)
                    .codeQualityAnalysis(codeQuality)
                    .testCaseAnalysis(testCase)
                    .allFindings(allFindings)
                    .generatedAt(now)
                    .build();

        } catch (Exception ex) {
            log.error("Unified analysis failed for prId={}: {}", pullRequestId, ex.getMessage(), ex);
            LocalDateTime failedAt = LocalDateTime.now();

            AnalysisResult failedResult = analysisResultRepository.save(AnalysisResult.builder()
                    .pullRequestId(pullRequestId)
                    .repositoryId(pullRequest.getRepositoryId())
                    .userId(pullRequest.getUserId())
                    .status("failed")
                    .summary("Unified analysis failed: " + ex.getMessage())
                    .rawOutput(Map.of("error", ex.getMessage()))
                    .createdAt(failedAt)
                    .completedAt(failedAt)
                    .build());

            pullRequest.setAnalysisStatus("failed");
            pullRequest.setAnalysisResultId(failedResult.getId());
            pullRequest.setAnalyzedAt(failedAt);
            pullRequest.setUpdatedAt(failedAt);
            pullRequestRepository.save(pullRequest);

            throw ex;
        }
    }

    public List<AnalysisResult> getResultsForPullRequest(String pullRequestId) {
        return analysisResultRepository.findByPullRequestId(pullRequestId);
    }

    private Map<String, Object> toMap(Object value) {
        return objectMapper.convertValue(value, new TypeReference<>() {
        });
    }

    private void addBugFindings(BugAnalysisResult bug, List<UnifiedFinding> allFindings, Map<String, Integer> countsBySeverity) {
        if (bug.getBugs() == null) {
            return;
        }
        for (BugFinding finding : bug.getBugs()) {
            allFindings.add(UnifiedFinding.builder()
                    .module("bug")
                    .id(finding.getId())
                    .severity(normalize(finding.getSeverity()))
                    .category(finding.getType())
                    .title(finding.getTitle())
                    .description(finding.getDescription())
                    .file(finding.getFile())
                    .startLine(finding.getStartLine())
                    .endLine(finding.getEndLine())
                    .recommendation(finding.getRecommendation())
                    .build());
            incrementSeverity(countsBySeverity, finding.getSeverity());
        }
    }

    private void addSecurityFindings(SecurityAnalysisResult security, List<UnifiedFinding> allFindings, Map<String, Integer> countsBySeverity) {
        if (security.getVulnerabilities() == null) {
            return;
        }
        for (SecurityVulnerability finding : security.getVulnerabilities()) {
            allFindings.add(UnifiedFinding.builder()
                    .module("security")
                    .id(finding.getId())
                    .severity(normalize(finding.getSeverity()))
                    .category(finding.getCategory())
                    .title(finding.getTitle())
                    .description(finding.getDescription())
                    .file(finding.getFile())
                    .startLine(finding.getStartLine())
                    .endLine(finding.getEndLine())
                    .recommendation(finding.getRecommendation())
                    .build());
            incrementSeverity(countsBySeverity, finding.getSeverity());
        }
    }

    private void addPerformanceFindings(PerformanceAnalysisResult performance, List<UnifiedFinding> allFindings, Map<String, Integer> countsBySeverity) {
        if (performance.getIssues() == null) {
            return;
        }
        for (PerformanceIssue finding : performance.getIssues()) {
            allFindings.add(UnifiedFinding.builder()
                    .module("performance")
                    .id(finding.getId())
                    .severity(normalize(finding.getSeverity()))
                    .category(finding.getCategory())
                    .title(finding.getTitle())
                    .description(finding.getDescription())
                    .file(finding.getFile())
                    .startLine(finding.getStartLine())
                    .endLine(finding.getEndLine())
                    .recommendation(finding.getRecommendation())
                    .build());
            incrementSeverity(countsBySeverity, finding.getSeverity());
        }
    }

    private void addCodeQualityFindings(CodeQualityAnalysisResult codeQuality, List<UnifiedFinding> allFindings, Map<String, Integer> countsBySeverity) {
        if (codeQuality.getSmells() == null) {
            return;
        }
        for (CodeSmellFinding finding : codeQuality.getSmells()) {
            allFindings.add(UnifiedFinding.builder()
                    .module("code_quality")
                    .id(finding.getId())
                    .severity(normalize(finding.getSeverity()))
                    .category(finding.getCategory())
                    .title(finding.getTitle())
                    .description(finding.getDescription())
                    .file(finding.getFile())
                    .startLine(finding.getStartLine())
                    .endLine(finding.getEndLine())
                    .recommendation(finding.getRecommendation())
                    .build());
            incrementSeverity(countsBySeverity, finding.getSeverity());
        }
    }

    private void addTestCaseFindings(TestCaseAnalysisResult testCase, List<UnifiedFinding> allFindings, Map<String, Integer> countsBySeverity) {
        if (testCase.getTestFindings() == null) {
            return;
        }
        for (TestCaseFinding finding : testCase.getTestFindings()) {
            allFindings.add(UnifiedFinding.builder()
                    .module("test_case")
                    .id(finding.getId())
                    .severity(normalize(finding.getSeverity()))
                    .category(finding.getCategory())
                    .title(finding.getTitle())
                    .description(finding.getDescription())
                    .file(finding.getFile())
                    .startLine(finding.getStartLine())
                    .endLine(finding.getEndLine())
                    .recommendation(finding.getSuggestedTest())
                    .build());
            incrementSeverity(countsBySeverity, finding.getSeverity());
        }
    }

    private void incrementSeverity(Map<String, Integer> countsBySeverity, String severity) {
        String key = normalize(severity);
        countsBySeverity.put(key, countsBySeverity.getOrDefault(key, 0) + 1);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase();
    }

    private double average(double... values) {
        if (values.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown";
    }

    private String maxRiskLevel(String... riskLevels) {
        String maxLevel = "none";
        int maxScore = 0;
        for (String level : riskLevels) {
            int score = riskScore(level);
            if (score > maxScore) {
                maxScore = score;
                maxLevel = normalize(level);
            }
        }
        return maxLevel;
    }

    private int riskScore(String riskLevel) {
        String level = normalize(riskLevel);
        return switch (level) {
            case "critical" -> 5;
            case "high" -> 4;
            case "medium" -> 3;
            case "low" -> 2;
            case "none" -> 1;
            default -> 0;
        };
    }

    private String buildSummary(
            BugAnalysisResult bug,
            SecurityAnalysisResult security,
            PerformanceAnalysisResult performance,
            CodeQualityAnalysisResult codeQuality,
            TestCaseAnalysisResult testCase,
            int totalFindings) {

        return String.format(
                "Unified analysis completed with %d findings. Bugs: %d, Security: %d, Performance: %d, Code quality: %d, Test gaps: %d.",
                totalFindings,
                Optional.ofNullable(bug.getBugs()).map(List::size).orElse(0),
                Optional.ofNullable(security.getVulnerabilities()).map(List::size).orElse(0),
                Optional.ofNullable(performance.getIssues()).map(List::size).orElse(0),
                Optional.ofNullable(codeQuality.getSmells()).map(List::size).orElse(0),
                Optional.ofNullable(testCase.getTestFindings()).map(List::size).orElse(0)
        );
    }
}