package com.aiprreview.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceIssue {

    private String id;                // PERF-001 …

    private String severity;          // critical|high|medium|low

    private String category;          // algorithm|database|memory|caching|concurrency|network|startup|other

    private String title;

    private String description;

    private String file;

    private Integer startLine;

    private Integer endLine;

    private String problematicCode;

    private String estimatedImpact;

    private String recommendation;
}
