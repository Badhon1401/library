// EnhancedQueryService.java
package com.ithra.library.service;

import com.ithra.library.dto.*;
import com.ithra.library.entity.*;
import com.ithra.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryService {

    private final DetectedPersonRepository personRepository;
    private final DetectedObjectRepository objectRepository;
    private final DetectedBookRepository bookRepository;
    private final MediaFileRepository mediaFileRepository;
    private final QueryHistoryRepository queryHistoryRepository;
    private final OpenAIService aiService;

    /**
     * Process natural language query with AI enhancement
     */
    @Transactional
    public QueryResponse processQuery(QueryRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Processing query: {} for media {}",
                    request.getQuery(), request.getMediaFileId());

            // Classify query type using AI if not provided
            if (request.getQueryType() == null) {
                request.setQueryType(aiService.classifyQuery(request.getQuery()));
            }

            // Get media context
            MediaFile mediaFile = mediaFileRepository.findById(request.getMediaFileId())
                    .orElseThrow(() -> new RuntimeException("Media file not found"));

            MediaAnalysisResult context = buildMediaContext(mediaFile);

            // Search for matches
            List<QueryMatch> matches = searchMatches(request, context);

            // Generate AI-enhanced answer
            String aiAnswer = aiService.enhanceQueryResponse(request, matches, context);

            // Calculate response time
            double responseTime = (System.currentTimeMillis() - startTime) / 1000.0;

            // Generate suggestions
            List<String> suggestions = aiService.generateQuerySuggestions(context);

            // Build response
            QueryResponse response = QueryResponse.builder()
                    .query(request.getQuery())
                    .found(!matches.isEmpty())
                    .answer(generateBasicAnswer(matches))
                    .aiEnhancedAnswer(aiAnswer)
                    .matches(matches)
                    .totalMatches(matches.size())
                    .confidence(calculateAverageConfidence(matches))
                    .responseTime(responseTime)
                    .timestamp(LocalDateTime.now())
                    .suggestions(suggestions)
                    .build();

            // Save query history
            saveQueryHistory(request, response, mediaFile);

            return response;

        } catch (Exception e) {
            log.error("Error processing query", e);
            throw new RuntimeException("Query processing failed: " + e.getMessage());
        }
    }

    /**
     * Search for matches based on query
     */
    private List<QueryMatch> searchMatches(QueryRequest request,
                                           MediaAnalysisResult context) {
        List<QueryMatch> matches = new ArrayList<>();
        String query = request.getQuery().toLowerCase();

        // Search people
        matches.addAll(searchPeople(request.getMediaFileId(), query, request.getTimeRange()));

        // Search objects
        matches.addAll(searchObjects(request.getMediaFileId(), query, request.getTimeRange()));

        // Search books
        matches.addAll(searchBooks(request.getMediaFileId(), query, request.getTimeRange()));

        // Sort by confidence
        matches.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

        return matches;
    }

    private List<QueryMatch> searchPeople(Long mediaFileId, String query, TimeRange timeRange) {
        List<QueryMatch> matches = new ArrayList<>();
        List<DetectedPerson> people = personRepository.findByMediaFileId(mediaFileId);

        for (DetectedPerson person : people) {
            // Check time range
            if (timeRange != null && person.getTimestamp() != null) {
                if (person.getTimestamp() < timeRange.getStartTime() ||
                        person.getTimestamp() > timeRange.getEndTime()) {
                    continue;
                }
            }

            // Match against query
            boolean matches_query = false;
            String description = "";

            if (query.contains("child") &&
                    person.getAgeCategory() == DetectedPerson.AgeCategory.CHILD) {
                matches_query = true;
                description = "Child detected";
            } else if (query.contains("adult") &&
                    person.getAgeCategory() == DetectedPerson.AgeCategory.ADULT) {
                matches_query = true;
                description = "Adult detected";
            } else if (query.contains("senior") &&
                    person.getAgeCategory() == DetectedPerson.AgeCategory.SENIOR) {
                matches_query = true;
                description = "Senior detected";
            }

            // Check emotions
            if (query.contains("happy") || query.contains("smiling")) {
                if (person.getEmotionalState() == DetectedPerson.EmotionalState.HAPPY) {
                    matches_query = true;
                    description = "Happy person detected";
                }
            } else if (query.contains("sad")) {
                if (person.getEmotionalState() == DetectedPerson.EmotionalState.SAD) {
                    matches_query = true;
                    description = "Sad person detected";
                }
            }

            // General person query
            if (query.contains("person") || query.contains("people")) {
                matches_query = true;
                description = String.format("%s %s person",
                        person.getEmotionalState(), person.getAgeCategory());
            }

            if (matches_query) {
                matches.add(QueryMatch.builder()
                        .type("PERSON")
                        .description(description)
                        .frameNumber(person.getFrameNumber())
                        .timestamp(person.getTimestamp())
                        .confidence(person.getConfidence())
                        .aiContext(person.getAiDescription())
                        .build());
            }
        }

        return matches;
    }

    private List<QueryMatch> searchObjects(Long mediaFileId, String query, TimeRange timeRange) {
        List<QueryMatch> matches = new ArrayList<>();
        List<DetectedObject> objects = objectRepository.findByMediaFileId(mediaFileId);

        for (DetectedObject object : objects) {
            // Check time range
            if (timeRange != null && object.getTimestamp() != null) {
                if (object.getTimestamp() < timeRange.getStartTime() ||
                        object.getTimestamp() > timeRange.getEndTime()) {
                    continue;
                }
            }

            // Match against query
            String objectName = object.getObjectName().toLowerCase();
            String category = object.getCategory().toLowerCase();

            if (query.contains(objectName) || query.contains(category) ||
                    objectName.contains(extractKeywords(query)[0])) {

                matches.add(QueryMatch.builder()
                        .type("OBJECT")
                        .description("Found " + object.getObjectName())
                        .frameNumber(object.getFrameNumber())
                        .timestamp(object.getTimestamp())
                        .confidence(object.getConfidence())
                        .aiContext(object.getAiDescription())
                        .build());
            }

            // Special queries
            if ((query.contains("drinking") || query.contains("coffee")) &&
                    (objectName.contains("cup") || objectName.contains("coffee") ||
                            objectName.contains("mug"))) {
                matches.add(QueryMatch.builder()
                        .type("OBJECT")
                        .description("Person may be drinking - " + object.getObjectName() + " detected")
                        .frameNumber(object.getFrameNumber())
                        .timestamp(object.getTimestamp())
                        .confidence(object.getConfidence())
                        .build());
            }
        }

        return matches;
    }

    private List<QueryMatch> searchBooks(Long mediaFileId, String query, TimeRange timeRange) {
        List<QueryMatch> matches = new ArrayList<>();
        List<DetectedBook> books = bookRepository.findByMediaFileId(mediaFileId);

        for (DetectedBook book : books) {
            // Check time range
            if (timeRange != null && book.getTimestamp() != null) {
                if (book.getTimestamp() < timeRange.getStartTime() ||
                        book.getTimestamp() > timeRange.getEndTime()) {
                    continue;
                }
            }

            // Match against query
            String bookName = book.getBookName() != null ? book.getBookName().toLowerCase() : "";
            String author = book.getAuthor() != null ? book.getAuthor().toLowerCase() : "";

            if (query.contains("book") || query.contains("reading") ||
                    query.contains(bookName) || query.contains(author)) {

                String description = String.format("Book: %s", book.getBookName());
                if (book.getAuthor() != null) {
                    description += " by " + book.getAuthor();
                }

                matches.add(QueryMatch.builder()
                        .type("BOOK")
                        .description(description)
                        .frameNumber(book.getFrameNumber())
                        .timestamp(book.getTimestamp())
                        .confidence(book.getConfidence())
                        .aiContext(book.getAiSummary())
                        .build());
            }
        }

        return matches;
    }

    /**
     * Get query history
     */
    @Cacheable(value = "queryHistory", key = "#mediaFileId")
    public List<QueryResponse> getQueryHistory(Long mediaFileId, int limit) {
        return queryHistoryRepository.findByMediaFileIdOrderByQueryTimeDesc(mediaFileId)
                .stream()
                .limit(limit)
                .map(this::convertToQueryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get query suggestions
     */
    public List<String> getQuerySuggestions(Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new RuntimeException("Media file not found"));

        MediaAnalysisResult context = buildMediaContext(mediaFile);
        return aiService.generateQuerySuggestions(context);
    }

    // Helper methods

    private MediaAnalysisResult buildMediaContext(MediaFile mediaFile) {
        List<DetectedPerson> people = personRepository.findByMediaFileId(mediaFile.getId());
        List<DetectedObject> objects = objectRepository.findByMediaFileId(mediaFile.getId());
        List<DetectedBook> books = bookRepository.findByMediaFileId(mediaFile.getId());

        return MediaAnalysisResult.builder()
                .mediaFileId(mediaFile.getId())
                .fileName(mediaFile.getFileName())
                .fileType(mediaFile.getFileType().name())
                .detectedPeople(people.stream().map(this::convertToPersonInfo).toList())
                .detectedObjects(objects.stream().map(this::convertToObjectInfo).toList())
                .detectedBooks(books.stream().map(this::convertToBookInfo).toList())
                .build();
    }

    private String generateBasicAnswer(List<QueryMatch> matches) {
        if (matches.isEmpty()) {
            return "No matches found for your query.";
        }

        Map<String, Long> typeCounts = matches.stream()
                .collect(Collectors.groupingBy(QueryMatch::getType, Collectors.counting()));

        StringBuilder answer = new StringBuilder();
        answer.append("Found ").append(matches.size()).append(" match(es): ");

        List<String> parts = new ArrayList<>();
        if (typeCounts.containsKey("PERSON")) {
            parts.add(typeCounts.get("PERSON") + " person(s)");
        }
        if (typeCounts.containsKey("OBJECT")) {
            parts.add(typeCounts.get("OBJECT") + " object(s)");
        }
        if (typeCounts.containsKey("BOOK")) {
            parts.add(typeCounts.get("BOOK") + " book(s)");
        }

        answer.append(String.join(", ", parts));

        return answer.toString();
    }

    private double calculateAverageConfidence(List<QueryMatch> matches) {
        if (matches.isEmpty()) return 0.0;

        return matches.stream()
                .mapToDouble(QueryMatch::getConfidence)
                .average()
                .orElse(0.0);
    }

    private void saveQueryHistory(QueryRequest request, QueryResponse response,
                                  MediaFile mediaFile) {
        QueryHistory history = QueryHistory.builder()
                .mediaFile(mediaFile)
                .query(request.getQuery())
                .answer(response.getAnswer())
                .aiResponse(response.getAiEnhancedAnswer())
                .matchCount(response.getTotalMatches())
                .responseTime(response.getResponseTime())
                .build();

        queryHistoryRepository.save(history);
    }

    private QueryResponse convertToQueryResponse(QueryHistory history) {
        return QueryResponse.builder()
                .query(history.getQuery())
                .answer(history.getAnswer())
                .aiEnhancedAnswer(history.getAiResponse())
                .totalMatches(history.getMatchCount())
                .responseTime(history.getResponseTime())
                .timestamp(history.getQueryTime())
                .build();
    }

    private String[] extractKeywords(String query) {
        String[] stopWords = {"is", "are", "the", "a", "an", "in", "on", "at",
                "any", "anyone", "there", "what", "when", "where", "how", "many"};

        return Arrays.stream(query.toLowerCase().split("\\s+"))
                .filter(word -> !Arrays.asList(stopWords).contains(word))
                .filter(word -> word.length() > 2)
                .toArray(String[]::new);
    }

    // Conversion methods
    private PersonInfo convertToPersonInfo(DetectedPerson person) {
        return PersonInfo.builder()
                .id(person.getId())
                .uniqueId(person.getUniqueId())
                .ageCategory(person.getAgeCategory().name())
                .estimatedAge(person.getEstimatedAge())
                .gender(person.getGender().name())
                .confidence(person.getConfidence())
                .emotionalState(person.getEmotionalState().name())
                .frameNumber(person.getFrameNumber())
                .timestamp(person.getTimestamp())
                .aiDescription(person.getAiDescription())
                .build();
    }

    private ObjectInfo convertToObjectInfo(DetectedObject object) {
        return ObjectInfo.builder()
                .id(object.getId())
                .objectName(object.getObjectName())
                .category(object.getCategory())
                .confidence(object.getConfidence())
                .frameNumber(object.getFrameNumber())
                .timestamp(object.getTimestamp())
                .aiDescription(object.getAiDescription())
                .build();
    }

    private BookInfo convertToBookInfo(DetectedBook book) {
        return BookInfo.builder()
                .id(book.getId())
                .bookName(book.getBookName())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .publicationYear(book.getPublicationYear())
                .uniqueId(book.getUniqueId())
                .confidence(book.getConfidence())
                .frameNumber(book.getFrameNumber())
                .timestamp(book.getTimestamp())
                .aiSummary(book.getAiSummary())
                .build();
    }
}