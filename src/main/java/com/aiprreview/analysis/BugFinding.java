package com.aiprreview.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugFinding {

    private String id;           // BUG-001, BUG-002 …

    private String severity;     // critical|high|medium|low

    private String type;         // null_reference|logic_error|resource_leak|concurrency|error_handling|off_by_one|other

    private String title;

    private String description;

    private String file;

    private Integer startLine;

    private Integer endLine;

    private String buggyCode;

    private String recommendation;

    private String testScenario;
}
