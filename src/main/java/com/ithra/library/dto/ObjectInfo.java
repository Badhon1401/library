package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectInfo {
    private Long id;
    private String objectName;
    private String category;
    private Double confidence;
    private Integer frameNumber;
    private Double timestamp;
    private BoundingBox boundingBox;
    private String aiDescription;
}
