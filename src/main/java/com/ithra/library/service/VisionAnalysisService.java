// EnhancedVisionAnalysisService.java
package com.ithra.library.service;

import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.protobuf.ByteString;
import com.ithra.library.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VisionAnalysisService {

    private final ImageAnnotatorClient visionClient;
    private final OpenAIService openAIService;

    public VisionAnalysisService(ImageAnnotatorClient visionClient,
                                         OpenAIService openAIService) {
        this.visionClient = visionClient;
        this.openAIService = openAIService;
    }

    /**
     * Comprehensive image analysis with all detection types
     */
    @Async
    public CompletableFuture<FrameAnalysisResult> analyzeFrame(byte[] imageBytes,
                                                               Integer frameNumber,
                                                               Double timestamp) {
        try {
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image img = Image.newBuilder().setContent(imgBytes).build();

            // Build comprehensive request with all features
            List<Feature> features = Arrays.asList(
                    Feature.newBuilder().setType(Type.FACE_DETECTION).build(),
                    Feature.newBuilder().setType(Type.OBJECT_LOCALIZATION).build(),
                    Feature.newBuilder().setType(Type.LABEL_DETECTION).build(),
                    Feature.newBuilder().setType(Type.TEXT_DETECTION).build(),
                    Feature.newBuilder().setType(Type.LOGO_DETECTION).build(),
                    Feature.newBuilder().setType(Type.LANDMARK_DETECTION).build(),
                    Feature.newBuilder().setType(Type.IMAGE_PROPERTIES).build()
            );

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addAllFeatures(features)
                    .setImage(img)
                    .build();

            BatchAnnotateImagesResponse response = visionClient
                    .batchAnnotateImages(Collections.singletonList(request));
            AnnotateImageResponse imageResponse = response.getResponsesList().get(0);

            if (imageResponse.hasError()) {
                log.error("Vision API error: {}", imageResponse.getError().getMessage());
                return CompletableFuture.completedFuture(new FrameAnalysisResult());
            }

            // Process all detections
            FrameAnalysisResult result = new FrameAnalysisResult();
            result.setFrameNumber(frameNumber);
            result.setTimestamp(timestamp);

            result.setPeople(detectPeople(imageResponse, frameNumber, timestamp));
            result.setObjects(detectObjects(imageResponse, frameNumber, timestamp));
            result.setBooks(detectBooks(imageResponse, frameNumber, timestamp));

            log.info("Frame {} analyzed: {} people, {} objects, {} books",
                    frameNumber, result.getPeople().size(),
                    result.getObjects().size(), result.getBooks().size());

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error analyzing frame", e);
            return CompletableFuture.completedFuture(new FrameAnalysisResult());
        }
    }

    /**
     * Detect and analyze people in frame
     */
    private List<PersonInfo> detectPeople(AnnotateImageResponse response,
                                          Integer frameNumber,
                                          Double timestamp) {
        List<PersonInfo> people = new ArrayList<>();

        for (FaceAnnotation face : response.getFaceAnnotationsList()) {
            PersonInfo person = PersonInfo.builder()
                    .uniqueId(UUID.randomUUID().toString())
                    .confidence((double) face.getDetectionConfidence())
                    .frameNumber(frameNumber)
                    .timestamp(timestamp)
                    .ageCategory(estimateAgeCategory(face))
                    .estimatedAge(estimateAge(face))
                    .gender(estimateGender(face))
                    .emotionalState(detectEmotion(face))
                    .boundingBox(convertBoundingBox(face.getBoundingPoly()))
                    .build();

            // Generate AI description
            try {
                Map<String, Object> personData = new HashMap<>();
                personData.put("age", person.getEstimatedAge());
                personData.put("emotion", person.getEmotionalState());
                personData.put("gender", person.getGender());

                String description = openAIService.generateEntityDescription(
                        "person", personData
                );
                person.setAiDescription(description);
            } catch (Exception e) {
                log.warn("Failed to generate AI description for person", e);
            }

            people.add(person);
        }

        return people;
    }

    /**
     * Detect objects with enhanced categorization
     */
    private List<ObjectInfo> detectObjects(AnnotateImageResponse response,
                                           Integer frameNumber,
                                           Double timestamp) {
        List<ObjectInfo> objects = new ArrayList<>();

        // Process localized objects
        for (LocalizedObjectAnnotation obj : response.getLocalizedObjectAnnotationsList()) {
            ObjectInfo objectInfo = ObjectInfo.builder()
                    .objectName(obj.getName())
                    .category(categorizeObject(obj.getName()))
                    .confidence((double) obj.getScore())
                    .frameNumber(frameNumber)
                    .timestamp(timestamp)
                    .boundingBox(convertBoundingBox(obj.getBoundingPoly()))
                    .build();

            objects.add(objectInfo);
        }

        // Process labels (general image understanding)
        for (EntityAnnotation label : response.getLabelAnnotationsList()) {
            if (label.getScore() > 0.75) {
                ObjectInfo objectInfo = ObjectInfo.builder()
                        .objectName(label.getDescription())
                        .category("LABEL")
                        .confidence((double) label.getScore())
                        .frameNumber(frameNumber)
                        .timestamp(timestamp)
                        .build();

                objects.add(objectInfo);
            }
        }

        return objects;
    }

    /**
     * Detect books with OCR and AI enhancement
     */
    private List<BookInfo> detectBooks(AnnotateImageResponse response,
                                       Integer frameNumber,
                                       Double timestamp) {
        List<BookInfo> books = new ArrayList<>();

        // Check for text in image
        if (response.getTextAnnotationsList().isEmpty()) {
            return books;
        }

        String fullText = response.getTextAnnotationsList().get(0).getDescription();

        // Look for book-related keywords
        if (containsBookKeywords(fullText)) {
            BookInfo book = BookInfo.builder()
                    .uniqueId(UUID.randomUUID().toString())
                    .bookName(extractTitle(fullText))
                    .author(extractAuthor(fullText))
                    .isbn(extractISBN(fullText))
                    .publisher(extractPublisher(fullText))
                    .publicationYear(extractYear(fullText))
                    .extractedText(fullText)
                    .confidence(0.80)
                    .frameNumber(frameNumber)
                    .timestamp(timestamp)
                    .build();

            // Generate AI summary
            try {
                String summary = openAIService.generateBookSummary(
                        book.getBookName(),
                        book.getAuthor(),
                        fullText,
                        book.getIsbn()
                );
                book.setAiSummary(summary);
            } catch (Exception e) {
                log.warn("Failed to generate book summary", e);
            }

            books.add(book);
        }

        return books;
    }

    // Helper methods

    private String estimateAgeCategory(FaceAnnotation face) {
        // Use joy likelihood as a proxy for age (children smile more)
        if (face.getJoyLikelihood() == Likelihood.VERY_LIKELY ||
                face.getJoyLikelihood() == Likelihood.LIKELY) {
            return "CHILD";
        }

        // Could use face landmarks for more accurate estimation
        return "ADULT";
    }

    private Integer estimateAge(FaceAnnotation face) {
        // Placeholder - could use ML model for accurate age estimation
        String category = estimateAgeCategory(face);
        switch (category) {
            case "CHILD": return 10;
            case "TEEN": return 16;
            case "ADULT": return 35;
            case "SENIOR": return 65;
            default: return 30;
        }
    }

    private String estimateGender(FaceAnnotation face) {
        // Google Vision API doesn't provide gender
        // Would need separate ML model
        return "UNKNOWN";
    }

    private String detectEmotion(FaceAnnotation face) {
        Map<String, Likelihood> emotions = new HashMap<>();
        emotions.put("HAPPY", face.getJoyLikelihood());
        emotions.put("SAD", face.getSorrowLikelihood());
        emotions.put("ANGRY", face.getAngerLikelihood());
        emotions.put("SURPRISED", face.getSurpriseLikelihood());

        return emotions.entrySet().stream()
                .filter(e -> e.getValue() == Likelihood.VERY_LIKELY ||
                        e.getValue() == Likelihood.LIKELY)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("NEUTRAL");
    }

    private String categorizeObject(String objectName) {
        String lower = objectName.toLowerCase();

        if (lower.contains("book") || lower.contains("magazine") ||
                lower.contains("journal")) {
            return "BOOK";
        }
        if (lower.contains("chair") || lower.contains("table") ||
                lower.contains("desk") || lower.contains("shelf")) {
            return "FURNITURE";
        }
        if (lower.contains("cup") || lower.contains("coffee") ||
                lower.contains("mug") || lower.contains("bottle")) {
            return "BEVERAGE";
        }
        if (lower.contains("laptop") || lower.contains("computer") ||
                lower.contains("phone") || lower.contains("tablet")) {
            return "ELECTRONICS";
        }

        return "GENERAL";
    }

    private boolean containsBookKeywords(String text) {
        String lower = text.toLowerCase();
        return lower.contains("isbn") ||
                lower.contains("author") ||
                lower.contains("publisher") ||
                lower.contains("edition") ||
                lower.contains("copyright");
    }

    private String extractTitle(String text) {
        String[] lines = text.split("\n");
        // First non-empty line is usually the title
        return Arrays.stream(lines)
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> line.length() > 3)
                .findFirst()
                .orElse("Unknown Title");
    }

    private String extractAuthor(String text) {
        // Look for "by" keyword
        String lower = text.toLowerCase();
        int byIndex = lower.indexOf("by ");

        if (byIndex != -1) {
            String afterBy = text.substring(byIndex + 3);
            String[] parts = afterBy.split("\n");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }

        return "Unknown Author";
    }

    private String extractISBN(String text) {
        // Match ISBN-10 or ISBN-13
        String[] words = text.split("\\s+");
        for (String word : words) {
            String clean = word.replaceAll("[^0-9]", "");
            if (clean.matches("\\d{10}") || clean.matches("\\d{13}")) {
                return clean;
            }
        }
        return null;
    }

    private String extractPublisher(String text) {
        // Look for common publisher keywords
        String[] lines = text.split("\n");
        for (String line : lines) {
            String lower = line.toLowerCase();
            if (lower.contains("press") || lower.contains("publishing") ||
                    lower.contains("publisher")) {
                return line.trim();
            }
        }
        return null;
    }

    private String extractYear(String text) {
        // Match 4-digit years (1900-2099)
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.matches("(19|20)\\d{2}")) {
                return word;
            }
        }
        return null;
    }

    private BoundingBox convertBoundingBox(BoundingPoly poly) {
        if (poly.getVerticesCount() == 0) {
            return null;
        }

        List<Vertex> vertices = poly.getVerticesList();

        int minX = vertices.stream().mapToInt(Vertex::getX).min().orElse(0);
        int minY = vertices.stream().mapToInt(Vertex::getY).min().orElse(0);
        int maxX = vertices.stream().mapToInt(Vertex::getX).max().orElse(0);
        int maxY = vertices.stream().mapToInt(Vertex::getY).max().orElse(0);

        return BoundingBox.builder()
                .x((double) minX)
                .y((double) minY)
                .width((double) (maxX - minX))
                .height((double) (maxY - minY))
                .build();
    }
}
