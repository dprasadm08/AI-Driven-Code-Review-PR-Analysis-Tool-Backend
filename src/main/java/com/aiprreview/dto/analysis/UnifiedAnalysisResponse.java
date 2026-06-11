package com.aiprreview.dto.analysis;

import com.aiprreview.analysis.BugAnalysisResult;
import com.aiprreview.analysis.CodeQualityAnalysisResult;
import com.aiprreview.analysis.PerformanceAnalysisResult;
import com.aiprreview.analysis.SecurityAnalysisResult;
import com.aiprreview.analysis.TestCaseAnalysisResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedAnalysisResponse {

    private String analysisResultId;

    private String pullRequestId;

    private String repositoryId;

    private String userId;

    private String provider;

    private String status;

    private String overallSummary;

    private String overallRiskLevel;

    private Integer totalFindings;

    private Double overallConfidence;

    private Map<String, Integer> countsByModule;

    private Map<String, Integer> countsBySeverity;

    private BugAnalysisResult bugAnalysis;

    private SecurityAnalysisResult securityAnalysis;

    private PerformanceAnalysisResult performanceAnalysis;

    private CodeQualityAnalysisResult codeQualityAnalysis;

    private TestCaseAnalysisResult testCaseAnalysis;

    private List<UnifiedFinding> allFindings;

    private LocalDateTime generatedAt;
}