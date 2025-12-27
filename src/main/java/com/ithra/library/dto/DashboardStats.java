package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {
    private OverallStats overall;
    private List<RecentActivity> recentActivities;
    private Map<String, Integer> detectionTrends;
    private List<TopQuery> topQueries;
    private LiveStreamStats liveStreamStats;
}
