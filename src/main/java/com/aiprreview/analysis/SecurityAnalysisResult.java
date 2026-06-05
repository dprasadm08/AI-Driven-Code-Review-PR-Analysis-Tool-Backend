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
public class SecurityAnalysisResult {

    private String summary;

    private int vulnerabilityCount;

    private String riskLevel;

    private List<SecurityVulnerability> vulnerabilities;

    private double confidence;

    private String analysisNotes;

    private String rawContent;

    private String provider;
}
