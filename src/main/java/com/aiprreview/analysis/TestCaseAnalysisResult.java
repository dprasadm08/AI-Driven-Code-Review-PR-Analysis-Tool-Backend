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
public class TestCaseAnalysisResult {

    private String summary;

    private int missingTestCount;

    private String coverageRiskLevel;

    private List<TestCaseFinding> testFindings;

    private double confidence;

    private String analysisNotes;

    private String rawContent;

    private String provider;
}
