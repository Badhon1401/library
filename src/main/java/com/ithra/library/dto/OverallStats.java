package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverallStats {
    private Long totalMediaFiles;
    private Long totalPeopleDetected;
    private Long totalObjectsDetected;
    private Long totalBooksDetected;
    private Long totalQueries;
    private Integer activeLiveStreams;
}
