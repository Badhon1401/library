package com.ithra.library.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.ithra.library.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class VisionAnalysisService {

    private final ImageAnnotatorClient vision;

    public VisionAnalysisService(ImageAnnotatorClient vision) {
        this.vision = vision; // injected singleton client
    }

    public List<PersonInfo> detectPeople(byte[] imageBytes) {
        List<PersonInfo> people = new ArrayList<>();

        try {
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image img = Image.newBuilder().setContent(imgBytes).build();

            Feature faceFeature = Feature.newBuilder().setType(Feature.Type.FACE_DETECTION).build();
            Feature labelFeature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(faceFeature)
                    .addFeatures(labelFeature)
                    .setImage(img)
                    .build();

            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
            AnnotateImageResponse res = response.getResponsesList().get(0);

            if (res.hasError()) {
                log.error("Error: " + res.getError().getMessage());
                return people;
            }

            for (FaceAnnotation face : res.getFaceAnnotationsList()) {
                PersonInfo person = new PersonInfo();
                person.setUniqueId(UUID.randomUUID().toString());
                person.setConfidence((double) face.getDetectionConfidence());
                person.setAgeCategory(estimateAgeCategory(face));
                person.setEstimatedAge(estimateAge(face));
                person.setEmotionalState(detectEmotion(face));
                person.setGender("UNKNOWN");
                people.add(person);
            }

            log.info("Detected {} people in image", people.size());
        } catch (Exception e) {
            log.error("Error detecting people", e);
        }

        return people;
    }

    public List<ObjectInfo> detectObjects(byte[] imageBytes) {
        List<ObjectInfo> objects = new ArrayList<>();

        try {
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image img = Image.newBuilder().setContent(imgBytes).build();

            Feature objectFeature = Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION).build();
            Feature labelFeature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(objectFeature)
                    .addFeatures(labelFeature)
                    .setImage(img)
                    .build();

            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
            AnnotateImageResponse res = response.getResponsesList().get(0);

            for (LocalizedObjectAnnotation obj : res.getLocalizedObjectAnnotationsList()) {
                ObjectInfo info = new ObjectInfo();
                info.setObjectName(obj.getName());
                info.setCategory(categorizeObject(obj.getName()));
                info.setConfidence((double) obj.getScore());
                objects.add(info);
            }

            for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
                if (annotation.getScore() > 0.7) {
                    ObjectInfo info = new ObjectInfo();
                    info.setObjectName(annotation.getDescription());
                    info.setCategory("GENERAL");
                    info.setConfidence((double) annotation.getScore());
                    objects.add(info);
                }
            }

            log.info("Detected {} objects in image", objects.size());
        } catch (Exception e) {
            log.error("Error detecting objects", e);
        }

        return objects;
    }

    public List<BookInfo> detectBooks(byte[] imageBytes) {
        List<BookInfo> books = new ArrayList<>();

        try {
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image img = Image.newBuilder().setContent(imgBytes).build();

            Feature textFeature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(textFeature)
                    .setImage(img)
                    .build();

            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
            AnnotateImageResponse res = response.getResponsesList().get(0);

            if (!res.getTextAnnotationsList().isEmpty()) {
                String detectedText = res.getTextAnnotationsList().get(0).getDescription();
                BookInfo book = extractBookInfo(detectedText);
                if (book != null) books.add(book);
            }

            log.info("Detected {} books in image", books.size());
        } catch (Exception e) {
            log.error("Error detecting books", e);
        }

        return books;
    }

    private String estimateAgeCategory(FaceAnnotation face) {
        return face.getJoyLikelihood() == Likelihood.VERY_LIKELY ? "CHILD" : "ADULT";
    }

    private Integer estimateAge(FaceAnnotation face) {
        return 30; // placeholder
    }

    private String detectEmotion(FaceAnnotation face) {
        if (face.getJoyLikelihood() == Likelihood.VERY_LIKELY || face.getJoyLikelihood() == Likelihood.LIKELY) return "HAPPY";
        if (face.getAngerLikelihood() == Likelihood.VERY_LIKELY) return "ANGRY";
        if (face.getSorrowLikelihood() == Likelihood.VERY_LIKELY) return "SAD";
        return "NEUTRAL";
    }

    private String categorizeObject(String name) {
        name = name.toLowerCase();
        if (name.contains("book")) return "BOOK";
        if (name.contains("cup") || name.contains("coffee") || name.contains("mug")) return "BEVERAGE";
        if (name.contains("chair") || name.contains("table") || name.contains("desk")) return "FURNITURE";
        return "GENERAL";
    }

    private BookInfo extractBookInfo(String text) {
        if (text.toLowerCase().contains("isbn") || text.toLowerCase().contains("author") || text.toLowerCase().contains("edition")) {
            BookInfo book = new BookInfo();
            book.setUniqueId(UUID.randomUUID().toString());
            book.setBookName(extractTitle(text));
            book.setAuthor(extractAuthor(text));
            book.setIsbn(extractISBN(text));
            book.setConfidence(0.75);
            return book;
        }
        return null;
    }

    private String extractTitle(String text) {
        String[] lines = text.split("\n");
        return lines.length > 0 ? lines[0] : "Unknown Title";
    }

    private String extractAuthor(String text) {
        if (text.toLowerCase().contains("by ")) {
            int index = text.toLowerCase().indexOf("by ");
            String[] parts = text.substring(index + 3).split("\n");
            return parts.length > 0 ? parts[0].trim() : "Unknown Author";
        }
        return "Unknown Author";
    }

    private String extractISBN(String text) {
        for (String word : text.split("\\s+")) {
            if (word.matches("\\d{10}|\\d{13}")) return word;
        }
        return null;
    }
}
