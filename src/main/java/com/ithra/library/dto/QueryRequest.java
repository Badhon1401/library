package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryRequest {
    private Long mediaFileId;
    private String query;
    private QueryType queryType;
    private List<String> filters;
    private TimeRange timeRange;

    public enum QueryType {
        GENERAL, COUNT, SEARCH, TEMPORAL, CONTEXTUAL
    }
}
