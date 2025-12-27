
// FrameAnalysisResult.java - Helper class
package com.ithra.library.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrameAnalysisResult {
    private Integer frameNumber;
    private Double timestamp;
    private List<PersonInfo> people;
    private List<ObjectInfo> objects;
    private List<BookInfo> books;
}