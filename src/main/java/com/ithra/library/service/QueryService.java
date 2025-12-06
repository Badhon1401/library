package com.ithra.library.service;

import com.ithra.library.dto.*;
import com.ithra.library.entity.*;
import com.ithra.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryService {

    private final DetectedPersonRepository personRepository;
    private final DetectedObjectRepository objectRepository;
    private final DetectedBookRepository bookRepository;

    public QueryResponse processQuery(QueryRequest request) {
        String query = request.getQuery().toLowerCase();
        List<QueryMatch> matches = new ArrayList<>();

        log.info("Processing query: {}", query);

        // Check for coffee/drinking queries
        if (query.contains("drinking") || query.contains("coffee") ||
                query.contains("cup") || query.contains("beverage")) {
            matches.addAll(searchForDrinking(request.getMediaFileId(), query));
        }

        // Check for reading/book queries
        if (query.contains("reading") || query.contains("book")) {
            matches.addAll(searchForReading(request.getMediaFileId()));
        }

        // Check for age-related queries
        if (query.contains("child") || query.contains("adult") ||
                query.contains("kid") || query.contains("senior")) {
            matches.addAll(searchByAge(request.getMediaFileId(), query));
        }

        // Check for emotion queries
        if (query.contains("happy") || query.contains("sad") ||
                query.contains("smiling") || query.contains("angry")) {
            matches.addAll(searchByEmotion(request.getMediaFileId(), query));
        }

        // Check for specific object queries
        if (query.contains("person") || query.contains("people") ||
                query.contains("how many")) {
            matches.addAll(searchForPeople(request.getMediaFileId()));
        }

        // General object search
        String[] keywords = extractKeywords(query);
        for (String keyword : keywords) {
            matches.addAll(searchForObject(request.getMediaFileId(), keyword));
        }

        // Remove duplicates
        matches = matches.stream()
                .distinct()
                .collect(Collectors.toList());

        QueryResponse response = new QueryResponse();
        response.setQuery(request.getQuery());
        response.setFound(!matches.isEmpty());
        response.setMatches(matches);
        response.setAnswer(generateAnswer(query, matches));

        return response;
    }

    private List<QueryMatch> searchForDrinking(Long mediaFileId, String query) {
        List<QueryMatch> matches = new ArrayList<>();

        List<DetectedObject> coffeeObjects = objectRepository
                .searchByKeyword("coffee", mediaFileId);
        coffeeObjects.addAll(objectRepository.searchByKeyword("cup", mediaFileId));
        coffeeObjects.addAll(objectRepository.searchByKeyword("mug", mediaFileId));

        for (DetectedObject obj : coffeeObjects) {
            QueryMatch match = new QueryMatch();
            match.setType("OBJECT");
            match.setDescription("Found " + obj.getObjectName());
            match.setFrameNumber(obj.getFrameNumber());
            match.setTimestamp(obj.getTimestamp());
            match.setConfidence(obj.getConfidence());
            matches.add(match);
        }

        return matches;
    }

    private List<QueryMatch> searchForReading(Long mediaFileId) {
        List<QueryMatch> matches = new ArrayList<>();

        List<DetectedBook> books = bookRepository.findByMediaFileId(mediaFileId);
        for (DetectedBook book : books) {
            QueryMatch match = new QueryMatch();
            match.setType("BOOK");
            match.setDescription("Found book: " + book.getBookName());
            match.setFrameNumber(book.getFrameNumber());
            match.setTimestamp(book.getTimestamp());
            match.setConfidence(book.getConfidence());
            matches.add(match);
        }

        return matches;
    }

    private List<QueryMatch> searchByAge(Long mediaFileId, String query) {
        List<QueryMatch> matches = new ArrayList<>();
        String ageCategory = null;

        if (query.contains("child") || query.contains("kid")) {
            ageCategory = "CHILD";
        } else if (query.contains("adult")) {
            ageCategory = "ADULT";
        } else if (query.contains("senior")) {
            ageCategory = "SENIOR";
        }

        if (ageCategory != null) {
            List<DetectedPerson> people = personRepository.findByMediaFileId(mediaFileId);
            for (DetectedPerson person : people) {
                if (ageCategory.equals(person.getAgeCategory())) {
                    QueryMatch match = new QueryMatch();
                    match.setType("PERSON");
                    match.setDescription("Found " + person.getAgeCategory() + " person");
                    match.setFrameNumber(person.getFrameNumber());
                    match.setTimestamp(person.getTimestamp());
                    match.setConfidence(person.getConfidence());
                    matches.add(match);
                }
            }
        }

        return matches;
    }

    private List<QueryMatch> searchByEmotion(Long mediaFileId, String query) {
        List<QueryMatch> matches = new ArrayList<>();
        String emotion = null;

        if (query.contains("happy") || query.contains("smiling")) {
            emotion = "HAPPY";
        } else if (query.contains("sad")) {
            emotion = "SAD";
        } else if (query.contains("angry")) {
            emotion = "ANGRY";
        }

        if (emotion != null) {
            List<DetectedPerson> people = personRepository.findByMediaFileId(mediaFileId);
            for (DetectedPerson person : people) {
                if (emotion.equals(person.getEmotionalState())) {
                    QueryMatch match = new QueryMatch();
                    match.setType("PERSON");
                    match.setDescription("Found " + person.getEmotionalState() + " person");
                    match.setFrameNumber(person.getFrameNumber());
                    match.setTimestamp(person.getTimestamp());
                    match.setConfidence(person.getConfidence());
                    matches.add(match);
                }
            }
        }

        return matches;
    }

    private List<QueryMatch> searchForPeople(Long mediaFileId) {
        List<QueryMatch> matches = new ArrayList<>();

        List<DetectedPerson> people = personRepository.findByMediaFileId(mediaFileId);
        for (DetectedPerson person : people) {
            QueryMatch match = new QueryMatch();
            match.setType("PERSON");
            match.setDescription("Person detected - " + person.getAgeCategory());
            match.setFrameNumber(person.getFrameNumber());
            match.setTimestamp(person.getTimestamp());
            match.setConfidence(person.getConfidence());
            matches.add(match);
        }

        return matches;
    }

    private List<QueryMatch> searchForObject(Long mediaFileId, String keyword) {
        List<QueryMatch> matches = new ArrayList<>();

        List<DetectedObject> objects = objectRepository
                .searchByKeyword(keyword, mediaFileId);

        for (DetectedObject obj : objects) {
            QueryMatch match = new QueryMatch();
            match.setType("OBJECT");
            match.setDescription("Found " + obj.getObjectName());
            match.setFrameNumber(obj.getFrameNumber());
            match.setTimestamp(obj.getTimestamp());
            match.setConfidence(obj.getConfidence());
            matches.add(match);
        }

        return matches;
    }

    private String[] extractKeywords(String query) {
        // Remove common words
        String[] stopWords = {"is", "are", "the", "a", "an", "in", "on", "at",
                "any", "anyone", "there", "what", "when", "where"};

        String[] words = query.toLowerCase().split("\\s+");
        List<String> keywords = new ArrayList<>();

        for (String word : words) {
            boolean isStopWord = false;
            for (String stopWord : stopWords) {
                if (word.equals(stopWord)) {
                    isStopWord = true;
                    break;
                }
            }
            if (!isStopWord && word.length() > 2) {
                keywords.add(word);
            }
        }

        return keywords.toArray(new String[0]);
    }

    private String generateAnswer(String query, List<QueryMatch> matches) {
        if (matches.isEmpty()) {
            return "No matches found for your query.";
        }

        StringBuilder answer = new StringBuilder();
        answer.append("Found ").append(matches.size()).append(" match(es). ");

        Map<String, Long> typeCount = matches.stream()
                .collect(Collectors.groupingBy(QueryMatch::getType, Collectors.counting()));

        if (typeCount.containsKey("PERSON")) {
            answer.append(typeCount.get("PERSON")).append(" person(s), ");
        }
        if (typeCount.containsKey("OBJECT")) {
            answer.append(typeCount.get("OBJECT")).append(" object(s), ");
        }
        if (typeCount.containsKey("BOOK")) {
            answer.append(typeCount.get("BOOK")).append(" book(s), ");
        }

        // Remove trailing comma and space
        if (answer.toString().endsWith(", ")) {
            answer.setLength(answer.length() - 2);
        }

        answer.append(".");

        // Add specific details for top matches
        if (!matches.isEmpty() && matches.get(0).getTimestamp() != null) {
            answer.append(" First occurrence at ").append(
                    String.format("%.2f", matches.get(0).getTimestamp())
            ).append(" seconds.");
        }

        return answer.toString();
    }
}