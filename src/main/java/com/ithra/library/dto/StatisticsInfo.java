package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticsInfo {
    private Integer totalPeople;
    private Integer totalObjects;
    private Integer totalBooks;

    private Map<String, Integer> peopleByAge;
    private Map<String, Integer> peopleByEmotion;
    private Map<String, Integer> objectsByCategory;

    private Double averageConfidence;
    private Integer uniquePeople;
}
