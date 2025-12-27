package com.ithra.library.controller;

import com.ithra.library.dto.LiveStreamRequest;
import com.ithra.library.dto.LiveStreamResponse;
import com.ithra.library.service.LiveStreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LiveStreamController {

    private final LiveStreamingService streamingService;

    /**
     * Start a new live stream
     */
    @PostMapping("/start")
    public ResponseEntity<LiveStreamResponse> startStream(
            @RequestBody LiveStreamRequest request) {
        try {
            log.info("Starting live stream: {}", request.getStreamName());

            LiveStreamResponse response = streamingService.startLiveStream(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error starting stream", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Stop a live stream
     */
    @PostMapping("/stop/{streamKey}")
    public ResponseEntity<Void> stopStream(@PathVariable String streamKey) {
        try {
            log.info("Stopping live stream: {}", streamKey);

            streamingService.stopLiveStream(streamKey);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error stopping stream", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get stream status
     */
    @GetMapping("/{streamKey}")
    public ResponseEntity<LiveStreamResponse> getStreamStatus(
            @PathVariable String streamKey) {
        try {
            LiveStreamResponse response = streamingService.getStreamStatus(streamKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting stream status", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List all active streams
     */
    @GetMapping("/active")
    public ResponseEntity<List<LiveStreamResponse>> getActiveStreams() {
        try {
            List<LiveStreamResponse> streams = streamingService.getActiveStreams();
            return ResponseEntity.ok(streams);
        } catch (Exception e) {
            log.error("Error listing active streams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Process external stream URL
     */
    @PostMapping("/process-url")
    public ResponseEntity<Void> processStreamUrl(
            @RequestParam String streamUrl,
            @RequestParam Long mediaFileId) {
        try {
            streamingService.processStreamUrl(streamUrl, mediaFileId);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("Error processing stream URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
