// DashboardService.java
package com.ithra.library.service;

import com.ithra.library.dto.*;
import com.ithra.library.entity.*;
import com.ithra.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final MediaFileRepository mediaFileRepository;
    private final DetectedPersonRepository personRepository;
    private final DetectedObjectRepository objectRepository;
    private final DetectedBookRepository bookRepository;
    private final QueryHistoryRepository queryHistoryRepository;
    private final LiveStreamRepository liveStreamRepository;

    /**
     * Get comprehensive dashboard statistics
     */
    @Cacheable(value = "dashboardStats", unless = "#result == null")
    public DashboardStats getDashboardStats() {
        log.info("Generating dashboard statistics");

        return DashboardStats.builder()
                .overall(getOverallStats())
                .recentActivities(getRecentActivities())
                .detectionTrends(getDetectionTrends())
                .topQueries(getTopQueries())
                .liveStreamStats(getLiveStreamStats())
                .build();
    }

    private OverallStats getOverallStats() {
        return OverallStats.builder()
                .totalMediaFiles(mediaFileRepository.count())
                .totalPeopleDetected(personRepository.count())
                .totalObjectsDetected(objectRepository.count())
                .totalBooksDetected(bookRepository.count())
                .totalQueries(queryHistoryRepository.count())
                .activeLiveStreams(liveStreamRepository.countActiveStreams().intValue())
                .build();
    }

    private List<RecentActivity> getRecentActivities() {
        List<RecentActivity> activities = new ArrayList<>();

        // Recent media uploads
        List<MediaFile> recentMedia = mediaFileRepository
                .findRecentMediaFiles(PageRequest.of(0, 10));

        for (MediaFile media : recentMedia) {
            activities.add(RecentActivity.builder()
                    .type(media.getFileType().name())
                    .description(String.format("Uploaded: %s", media.getFileName()))
                    .timestamp(media.getUploadDate())
                    .mediaFileId(media.getId())
                    .build());
        }

        // Recent queries
        LocalDateTime last24Hours = LocalDateTime.now().minusDays(1);
        List<QueryHistory> recentQueries = queryHistoryRepository
                .findByQueryTimeBetween(last24Hours, LocalDateTime.now());

        recentQueries.stream()
                .limit(10)
                .forEach(query -> activities.add(RecentActivity.builder()
                        .type("QUERY")
                        .description(String.format("Query: %s", query.getQuery()))
                        .timestamp(query.getQueryTime())
                        .mediaFileId(query.getMediaFile().getId())
                        .build()));

        // Sort by timestamp
        activities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return activities.stream().limit(20).collect(Collectors.toList());
    }

    private Map<String, Integer> getDetectionTrends() {
        Map<String, Integer> trends = new HashMap<>();

        // Get counts by category
        List<DetectedObject> allObjects = objectRepository.findAll();
        Map<String, Long> categoryCounts = allObjects.stream()
                .collect(Collectors.groupingBy(
                        DetectedObject::getCategory,
                        Collectors.counting()
                ));

        categoryCounts.forEach((key, value) -> trends.put(key, value.intValue()));

        return trends;
    }

    private List<TopQuery> getTopQueries() {
        List<Object[]> topQueriesData = queryHistoryRepository.findTopQueries();

        return topQueriesData.stream()
                .limit(10)
                .map(data -> TopQuery.builder()
                        .query((String) data[0])
                        .count(((Long) data[1]).intValue())
                        .avgResponseTime(calculateAvgResponseTime((String) data[0]))
                        .build())
                .collect(Collectors.toList());
    }

    private LiveStreamStats getLiveStreamStats() {
        Long activeStreams = liveStreamRepository.countActiveStreams();
        Long totalViewers = liveStreamRepository.sumActiveViewers();

        List<LiveStream> allStreams = liveStreamRepository.findAll();
        Long totalDuration = allStreams.stream()
                .filter(s -> s.getDurationSeconds() != null)
                .mapToLong(LiveStream::getDurationSeconds)
                .sum();

        if (totalViewers == null) {
            totalViewers = 0L;
        }

        Double avgViewers = activeStreams > 0 ?
                (double) totalViewers / activeStreams : 0.0;

        return LiveStreamStats.builder()
                .activeStreams(activeStreams.intValue())
                .totalViewers(totalViewers.intValue())
                .totalDuration(totalDuration)
                .avgViewersPerStream(avgViewers)
                .build();
    }

    /**
     * Get detection trends for a specific period
     */
    public Map<String, Object> getDetectionTrends(String period) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate;

        switch (period != null ? period : "day") {
            case "week":
                startDate = endDate.minusWeeks(1);
                break;
            case "month":
                startDate = endDate.minusMonths(1);
                break;
            case "year":
                startDate = endDate.minusYears(1);
                break;
            default:
                startDate = endDate.minusDays(1);
        }

        List<MediaFile> mediaFiles = mediaFileRepository
                .findByUploadDateBetween(startDate, endDate);

        Map<String, Object> trends = new HashMap<>();

        // Calculate daily trends
        Map<String, Integer> dailyUploads = new HashMap<>();
        Map<String, Integer> dailyDetections = new HashMap<>();

        for (MediaFile media : mediaFiles) {
            String day = media.getUploadDate().toLocalDate().toString();
            dailyUploads.merge(day, 1, Integer::sum);

            int detectionCount = media.getPeopleCount() != null ? media.getPeopleCount() : 0;
            detectionCount += media.getObjectsCount() != null ? media.getObjectsCount() : 0;
            detectionCount += media.getBooksCount() != null ? media.getBooksCount() : 0;

            dailyDetections.merge(day, detectionCount, Integer::sum);
        }

        trends.put("dailyUploads", dailyUploads);
        trends.put("dailyDetections", dailyDetections);
        trends.put("totalFiles", mediaFiles.size());

        return trends;
    }

    private Double calculateAvgResponseTime(String query) {
        return queryHistoryRepository.findAverageResponseTime();
    }
}
