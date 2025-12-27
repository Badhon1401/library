package com.ithra.library.controller;

import com.ithra.library.dto.DashboardStats;
import com.ithra.library.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        try {
            DashboardStats stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting dashboard stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get detection trends
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getDetectionTrends(
            @RequestParam(required = false) String period) {
        try {
            var trends = dashboardService.getDetectionTrends(period);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error getting trends", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
