package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaAnalysisResult {
    private Long mediaFileId;
    private String fileName;
    private String fileType;
    private LocalDateTime uploadDate;
    private String status;
    private List<PersonInfo> detectedPeople;
    private List<ObjectInfo> detectedObjects;
    private List<BookInfo> detectedBooks;
    private String summary;
}

