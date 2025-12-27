package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopQuery {
    private String query;
    private Integer count;
    private Double avgResponseTime;
}
