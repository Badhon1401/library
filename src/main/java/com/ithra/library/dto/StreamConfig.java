package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamConfig {
    private Integer width;
    private Integer height;
    private Integer frameRate;
    private Integer bitrate;
    private Boolean enableAnalysis;
    private Integer analysisInterval; // frames
}
