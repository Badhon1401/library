package com.ithra.library.controller;

import com.ithra.library.dto.QueryRequest;
import com.ithra.library.dto.QueryResponse;
import com.ithra.library.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QueryController {

    private final QueryService queryService;

    /**
     * Process natural language query
     */
    @PostMapping
    public ResponseEntity<QueryResponse> processQuery(
            @RequestBody QueryRequest request) {
        try {
            log.info("Processing query: {}", request.getQuery());

            QueryResponse response = queryService.processQuery(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get query history for media file
     */
    @GetMapping("/history/{mediaFileId}")
    public ResponseEntity<List<QueryResponse>> getQueryHistory(
            @PathVariable Long mediaFileId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<QueryResponse> history = queryService
                    .getQueryHistory(mediaFileId, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting query history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get query suggestions
     */
    @GetMapping("/suggestions/{mediaFileId}")
    public ResponseEntity<List<String>> getQuerySuggestions(
            @PathVariable Long mediaFileId) {
        try {
            List<String> suggestions = queryService.getQuerySuggestions(mediaFileId);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Error getting suggestions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
