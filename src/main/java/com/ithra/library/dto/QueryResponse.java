package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryResponse {
    private String query;
    private Boolean found;
    private String answer;
    private String aiEnhancedAnswer;
    private List<QueryMatch> matches;
    private Integer totalMatches;
    private Double confidence;
    private Double responseTime;
    private LocalDateTime timestamp;
    private List<String> suggestions;
}
