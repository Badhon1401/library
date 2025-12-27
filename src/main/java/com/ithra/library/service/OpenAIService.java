// OpenAIService.java - ChatGPT Integration
package com.ithra.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ithra.library.dto.*;
import com.ithra.library.entity.DetectedObject;
import com.ithra.library.entity.DetectedPerson;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OpenAIService {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    @Value("${app.openai.model}")
    private String model;

    @Value("${app.openai.max-tokens}")
    private Integer maxTokens;

    @Value("${app.openai.temperature}")
    private Double temperature;

    public OpenAIService(@Value("${app.openai.api-key}") String apiKey,
                         ObjectMapper objectMapper) {
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(60));
        this.objectMapper = objectMapper;
    }

    /**
     * Generate AI summary for media content
     */
    public String generateMediaSummary(MediaAnalysisResult analysis) {
        try {
            String prompt = buildSummaryPrompt(analysis);
            return callChatGPT(prompt, "You are an expert media analyst.");
        } catch (Exception e) {
            log.error("Error generating media summary", e);
            return "Unable to generate AI summary";
        }
    }

    /**
     * Generate detailed description for detected entities
     */
    public String generateEntityDescription(String entityType, Map<String, Object> entityData) {
        try {
            String prompt = String.format(
                    "Provide a natural, detailed description of this %s: %s",
                    entityType,
                    objectMapper.writeValueAsString(entityData)
            );
            return callChatGPT(prompt, "You are a helpful assistant.");
        } catch (Exception e) {
            log.error("Error generating entity description", e);
            return "No description available";
        }
    }

    /**
     * Process natural language query with AI enhancement
     */
    public String enhanceQueryResponse(QueryRequest request,
                                       List<QueryMatch> matches,
                                       MediaAnalysisResult context) {
        try {
            String prompt = buildQueryEnhancementPrompt(request, matches, context);
            return callChatGPT(prompt,
                    "You are an intelligent library assistant helping users understand their media content.");
        } catch (Exception e) {
            log.error("Error enhancing query response", e);
            return generateFallbackAnswer(matches);
        }
    }

    /**
     * Generate book summary from extracted text
     */
    @Cacheable(value = "bookSummaries", key = "#isbn")
    public String generateBookSummary(String bookName, String author,
                                      String extractedText, String isbn) {
        try {
            String prompt = String.format(
                    "Provide a concise summary of the book '%s' by %s. " +
                            "Extracted text: %s",
                    bookName, author,
                    truncateText(extractedText, 1000)
            );
            return callChatGPT(prompt, "You are a knowledgeable librarian.");
        } catch (Exception e) {
            log.error("Error generating book summary", e);
            return "Summary not available";
        }
    }

    /**
     * Analyze temporal patterns and behaviors
     */
    public String analyzeTemporalPatterns(List<DetectedPerson> people,
                                          List<DetectedObject> objects) {
        try {
            String prompt = buildTemporalAnalysisPrompt(people, objects);
            return callChatGPT(prompt,
                    "You are a behavioral analyst examining patterns in library activity.");
        } catch (Exception e) {
            log.error("Error analyzing temporal patterns", e);
            return "Pattern analysis unavailable";
        }
    }

    /**
     * Generate query suggestions based on content
     */
    public List<String> generateQuerySuggestions(MediaAnalysisResult analysis) {
        try {
            String prompt = buildSuggestionPrompt(analysis);
            String response = callChatGPT(prompt,
                    "You are a helpful assistant suggesting relevant questions.");

            return Arrays.stream(response.split("\n"))
                    .filter(s -> !s.trim().isEmpty())
                    .map(s -> s.replaceAll("^[0-9]+\\.\\s*", ""))
                    .limit(5)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error generating suggestions", e);
            return List.of(
                    "How many people are in the library?",
                    "What books are visible?",
                    "Are there any children present?"
            );
        }
    }

    /**
     * Classify query type using AI
     */
    public QueryRequest.QueryType classifyQuery(String query) {
        try {
            String prompt = String.format(
                    "Classify this query into one of: GENERAL, COUNT, SEARCH, TEMPORAL, CONTEXTUAL. " +
                            "Query: '%s'. Reply with only the classification.", query
            );
            String classification = callChatGPT(prompt, "You are a query classifier.");
            return QueryRequest.QueryType.valueOf(classification.trim());
        } catch (Exception e) {
            log.warn("Error classifying query, using GENERAL", e);
            return QueryRequest.QueryType.GENERAL;
        }
    }

    /**
     * Determine person's activity from context
     */
    public String inferPersonActivity(PersonInfo person,
                                      List<ObjectInfo> nearbyObjects,
                                      List<BookInfo> nearbyBooks) {
        try {
            String prompt = String.format(
                    "Based on this context, what is the person likely doing? " +
                            "Person: %s years old, %s, feeling %s. " +
                            "Nearby objects: %s. Nearby books: %s.",
                    person.getEstimatedAge(),
                    person.getAgeCategory(),
                    person.getEmotionalState(),
                    nearbyObjects.stream().map(ObjectInfo::getObjectName)
                            .collect(Collectors.joining(", ")),
                    nearbyBooks.stream().map(BookInfo::getBookName)
                            .collect(Collectors.joining(", "))
            );
            return callChatGPT(prompt, "You are observing library activity.");
        } catch (Exception e) {
            return "Activity unknown";
        }
    }

    // Private helper methods

    private String callChatGPT(String prompt, String systemMessage) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(Arrays.asList(
                        new ChatMessage("system", systemMessage),
                        new ChatMessage("user", prompt)
                ))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

        ChatCompletionResult result = openAiService.createChatCompletion(request);
        return result.getChoices().get(0).getMessage().getContent();
    }

    private String buildSummaryPrompt(MediaAnalysisResult analysis) {
        return String.format(
                "Generate a comprehensive summary of this library media analysis:\n" +
                        "- File: %s (%s)\n" +
                        "- People detected: %d\n" +
                        "- Objects detected: %d\n" +
                        "- Books detected: %d\n" +
                        "- Duration: %s\n\n" +
                        "Provide insights about the library activity, notable observations, and key highlights.",
                analysis.getFileName(),
                analysis.getFileType(),
                analysis.getDetectedPeople().size(),
                analysis.getDetectedObjects().size(),
                analysis.getDetectedBooks().size(),
                formatDuration(analysis.getDuration())
        );
    }

    private String buildQueryEnhancementPrompt(QueryRequest request,
                                               List<QueryMatch> matches,
                                               MediaAnalysisResult context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("User query: ").append(request.getQuery()).append("\n\n");
        prompt.append("Search results:\n");

        for (QueryMatch match : matches) {
            prompt.append(String.format("- %s: %s (%.0f%% confidence)",
                    match.getType(), match.getDescription(), match.getConfidence() * 100));
            if (match.getTimestamp() != null) {
                prompt.append(String.format(" at %.2fs", match.getTimestamp()));
            }
            prompt.append("\n");
        }

        prompt.append("\nMedia context:\n");
        prompt.append(String.format("Total people: %d, Total objects: %d, Total books: %d\n",
                context.getDetectedPeople().size(),
                context.getDetectedObjects().size(),
                context.getDetectedBooks().size()));

        prompt.append("\nProvide a natural, conversational answer to the user's query " +
                "based on the search results and context.");

        return prompt.toString();
    }

    private String buildTemporalAnalysisPrompt(List<DetectedPerson> people,
                                               List<DetectedObject> objects) {
        return String.format(
                "Analyze the temporal patterns in this library footage:\n" +
                        "- %d people detected across %d unique timestamps\n" +
                        "- %d objects detected\n" +
                        "Identify patterns, peak activity times, and interesting behaviors.",
                people.size(),
                people.stream().map(p -> p.getTimestamp()).distinct().count(),
                objects.size()
        );
    }

    private String buildSuggestionPrompt(MediaAnalysisResult analysis) {
        return String.format(
                "Based on this media analysis, suggest 5 relevant questions a user might ask:\n" +
                        "- %d people detected\n" +
                        "- %d objects detected\n" +
                        "- %d books detected\n" +
                        "Format each suggestion as a numbered list.",
                analysis.getDetectedPeople().size(),
                analysis.getDetectedObjects().size(),
                analysis.getDetectedBooks().size()
        );
    }

    private String generateFallbackAnswer(List<QueryMatch> matches) {
        if (matches.isEmpty()) {
            return "No matches found for your query.";
        }

        return String.format("Found %d match(es): %s",
                matches.size(),
                matches.stream()
                        .map(QueryMatch::getDescription)
                        .limit(3)
                        .collect(Collectors.joining(", ")));
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null) return "N/A";
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
}