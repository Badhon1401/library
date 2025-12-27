package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonInfo {
    private Long id;
    private String uniqueId;
    private String ageCategory;
    private Integer estimatedAge;
    private String gender;
    private Double confidence;
    private String emotionalState;
    private Integer frameNumber;
    private Double timestamp;
    private BoundingBox boundingBox;
    private String aiDescription;
}
