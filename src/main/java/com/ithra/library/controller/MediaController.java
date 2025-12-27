// MediaController.java - REST API
package com.ithra.library.controller;

import com.ithra.library.dto.*;
import com.ithra.library.entity.MediaFile;
import com.ithra.library.service.MediaAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MediaController {

    private final MediaAnalysisService mediaAnalysisService;

    /**
     * Upload and analyze media file
     */
    @PostMapping("/upload")
    public ResponseEntity<MediaAnalysisResult> uploadFile(
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            MediaFile mediaFile = mediaAnalysisService.uploadFile(file);
            MediaAnalysisResult result = mediaAnalysisService
                    .getAnalysisResult(mediaFile.getId());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get analysis result by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MediaAnalysisResult> getAnalysisResult(@PathVariable Long id) {
        try {
            MediaAnalysisResult result = mediaAnalysisService.getAnalysisResult(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting analysis result", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List all media files with pagination
     */
    @GetMapping
    public ResponseEntity<Page<MediaAnalysisResult>> listMediaFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String status) {
        try {
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by("uploadDate").descending());

            Page<MediaAnalysisResult> results = mediaAnalysisService
                    .listMediaFiles(fileType, status, pageable);

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error listing media files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete media file
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMediaFile(@PathVariable Long id) {
        try {
            mediaAnalysisService.deleteMediaFile(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting media file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get media statistics
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<StatisticsInfo> getStatistics(@PathVariable Long id) {
        try {
            StatisticsInfo stats = mediaAnalysisService.getStatistics(id);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting statistics", e);
            return ResponseEntity.notFound().build();
        }
    }
}
