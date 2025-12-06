package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectInfo {
    private Long id;
    private String objectName;
    private String category;
    private Double confidence;
    private Integer frameNumber;
    private Double timestamp;
}
