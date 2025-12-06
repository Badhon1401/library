package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class
QueryResponse {
    private String query;
    private boolean found;
    private String answer;
    private List<QueryMatch> matches;
}
