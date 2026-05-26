package com.aiprreview.dto.pullrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestFileResponse {

    private String filename;
    
    private String status; // added, removed, modified, renamed
    
    private Integer additions;
    
    private Integer deletions;
    
    private Integer changes;
    
    private String patch; // The unified diff patch
    
    private String previousFilename; // For renamed files
    
    private String blobUrl;
    
    private String rawUrl;
}
