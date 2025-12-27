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
public class MediaAnalysisResult {
    private Long mediaFileId;
    private String fileName;
    private String fileType;
    private LocalDateTime uploadDate;
    private String status;
    private Boolean isLive;
    private String streamUrl;
    private String hlsPlaylistUrl;

    private Integer totalFramesProcessed;
    private Integer duration;
    private Double frameRate;

    private List<PersonInfo> detectedPeople;
    private List<ObjectInfo> detectedObjects;
    private List<BookInfo> detectedBooks;

    private String aiSummary;
    private String aiDescription;

    private StatisticsInfo statistics;
}
