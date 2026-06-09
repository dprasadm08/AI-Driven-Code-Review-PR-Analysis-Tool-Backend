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
public class CodeQualityAnalysisResult {

    private String summary;

    private int smellCount;

    private Integer qualityScore;

    private String riskLevel;

    private List<CodeSmellFinding> smells;

    private double confidence;

    private String analysisNotes;

    private String rawContent;

    private String provider;
}
