package com.aiprreview.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceAnalysisResult {

    private String summary;

    private int issueCount;

    private String riskLevel;         // none|low|medium|high|critical

    private List<PerformanceIssue> issues;

    private double confidence;

    private String analysisNotes;

    private String rawContent;        // original AI response for audit

    private String provider;          // which AI provider produced this
}
