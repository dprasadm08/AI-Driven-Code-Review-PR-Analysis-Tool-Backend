package com.aiprreview.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSmellFinding {

    private String id;

    private String severity;

    private String category;

    private String title;

    private String description;

    private String file;

    private Integer startLine;

    private Integer endLine;

    private String codeSnippet;

    private String recommendation;
}
