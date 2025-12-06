package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryMatch {
    private String type; // PERSON, OBJECT, BOOK
    private String description;
    private Integer frameNumber;
    private Double timestamp;
    private Double confidence;
}
