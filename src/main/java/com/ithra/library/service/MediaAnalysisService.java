package com.ithra.library.service;

import com.ithra.library.dto.*;
import com.ithra.library.entity.*;
import com.ithra.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaAnalysisService {

    private final MediaFileRepository mediaFileRepository;
    private final DetectedPersonRepository personRepository;
    private final DetectedObjectRepository objectRepository;
    private final DetectedBookRepository bookRepository;
    private final VisionAnalysisService visionService;
    private final VideoProcessingService videoService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public MediaFile uploadFile(MultipartFile file) throws Exception {
        // Create upload directory if not exists
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Save file
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create media file entity
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileName(file.getOriginalFilename());
        mediaFile.setFilePath(filePath.toString());
        mediaFile.setFileSize(file.getSize());
        mediaFile.setUploadDate(LocalDateTime.now());
        mediaFile.setStatus("PROCESSING");

        // Determine file type
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("video")) {
            mediaFile.setFileType("VIDEO");
        } else {
            mediaFile.setFileType("IMAGE");
        }

        mediaFile = mediaFileRepository.save(mediaFile);

        // Start async processing
        processMediaAsync(mediaFile.getId());

        return mediaFile;
    }

    @Async
    @Transactional
    public void processMediaAsync(Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new RuntimeException("Media file not found"));

        try {
            if ("IMAGE".equals(mediaFile.getFileType())) {
                processImage(mediaFile);
            } else if ("VIDEO".equals(mediaFile.getFileType())) {
                processVideo(mediaFile);
            }

            mediaFile.setStatus("COMPLETED");
            mediaFileRepository.save(mediaFile);

        } catch (Exception e) {
            log.error("Error processing media file", e);
            mediaFile.setStatus("FAILED");
            mediaFileRepository.save(mediaFile);
        }
    }

    private void processImage(MediaFile mediaFile) throws Exception {
        byte[] imageBytes = Files.readAllBytes(Paths.get(mediaFile.getFilePath()));

        // Detect people
        List<PersonInfo> people = visionService.detectPeople(imageBytes);
        for (PersonInfo personInfo : people) {
            DetectedPerson person = new DetectedPerson();
            person.setMediaFile(mediaFile);
            person.setUniqueId(personInfo.getUniqueId());
            person.setAgeCategory(personInfo.getAgeCategory());
            person.setEstimatedAge(personInfo.getEstimatedAge());
            person.setGender(personInfo.getGender());
            person.setConfidence(personInfo.getConfidence());
            person.setEmotionalState(personInfo.getEmotionalState());
            personRepository.save(person);
        }

        // Detect objects
        List<ObjectInfo> objects = visionService.detectObjects(imageBytes);
        for (ObjectInfo objectInfo : objects) {
            DetectedObject object = new DetectedObject();
            object.setMediaFile(mediaFile);
            object.setObjectName(objectInfo.getObjectName());
            object.setCategory(objectInfo.getCategory());
            object.setConfidence(objectInfo.getConfidence());
            objectRepository.save(object);
        }

        // Detect books
        List<BookInfo> books = visionService.detectBooks(imageBytes);
        for (BookInfo bookInfo : books) {
            DetectedBook book = new DetectedBook();
            book.setMediaFile(mediaFile);
            book.setBookName(bookInfo.getBookName());
            book.setAuthor(bookInfo.getAuthor());
            book.setIsbn(bookInfo.getIsbn());
            book.setUniqueId(bookInfo.getUniqueId());
            book.setConfidence(bookInfo.getConfidence());
            bookRepository.save(book);
        }

        log.info("Image processing completed for file: {}", mediaFile.getFileName());
    }

    private void processVideo(MediaFile mediaFile) throws Exception {
        List<VideoProcessingService.VideoFrame> frames =
                videoService.extractFrames(mediaFile.getFilePath());

        for (VideoProcessingService.VideoFrame frame : frames) {
            // Detect people in frame
            List<PersonInfo> people = visionService.detectPeople(frame.getImageBytes());
            for (PersonInfo personInfo : people) {
                DetectedPerson person = new DetectedPerson();
                person.setMediaFile(mediaFile);
                person.setUniqueId(personInfo.getUniqueId());
                person.setAgeCategory(personInfo.getAgeCategory());
                person.setEstimatedAge(personInfo.getEstimatedAge());
                person.setGender(personInfo.getGender());
                person.setConfidence(personInfo.getConfidence());
                person.setEmotionalState(personInfo.getEmotionalState());
                person.setFrameNumber(frame.getFrameNumber());
                person.setTimestamp(frame.getTimestamp());
                personRepository.save(person);
            }

            // Detect objects in frame
            List<ObjectInfo> objects = visionService.detectObjects(frame.getImageBytes());
            for (ObjectInfo objectInfo : objects) {
                DetectedObject object = new DetectedObject();
                object.setMediaFile(mediaFile);
                object.setObjectName(objectInfo.getObjectName());
                object.setCategory(objectInfo.getCategory());
                object.setConfidence(objectInfo.getConfidence());
                object.setFrameNumber(frame.getFrameNumber());
                object.setTimestamp(frame.getTimestamp());
                objectRepository.save(object);
            }

            // Detect books in frame
            List<BookInfo> books = visionService.detectBooks(frame.getImageBytes());
            for (BookInfo bookInfo : books) {
                DetectedBook book = new DetectedBook();
                book.setMediaFile(mediaFile);
                book.setBookName(bookInfo.getBookName());
                book.setAuthor(bookInfo.getAuthor());
                book.setIsbn(bookInfo.getIsbn());
                book.setUniqueId(bookInfo.getUniqueId());
                book.setConfidence(bookInfo.getConfidence());
                book.setFrameNumber(frame.getFrameNumber());
                book.setTimestamp(frame.getTimestamp());
                bookRepository.save(book);
            }
        }

        log.info("Video processing completed for file: {}", mediaFile.getFileName());
    }

    @Transactional(readOnly = true)
    public MediaAnalysisResult getAnalysisResult(Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new RuntimeException("Media file not found"));

        List<DetectedPerson> people = personRepository.findByMediaFileId(mediaFileId);
        List<DetectedObject> objects = objectRepository.findByMediaFileId(mediaFileId);
        List<DetectedBook> books = bookRepository.findByMediaFileId(mediaFileId);

        MediaAnalysisResult result = new MediaAnalysisResult();
        result.setMediaFileId(mediaFile.getId());
        result.setFileName(mediaFile.getFileName());
        result.setFileType(mediaFile.getFileType());
        result.setUploadDate(mediaFile.getUploadDate());
        result.setStatus(mediaFile.getStatus());

        // Convert entities to DTOs
        result.setDetectedPeople(people.stream().map(this::convertToPersonInfo).collect(Collectors.toList()));
        result.setDetectedObjects(objects.stream().map(this::convertToObjectInfo).collect(Collectors.toList()));
        result.setDetectedBooks(books.stream().map(this::convertToBookInfo).collect(Collectors.toList()));

        // Generate summary
        result.setSummary(generateSummary(people.size(), objects.size(), books.size()));

        return result;
    }

    public List<MediaFile> getAllMediaFiles() {
        return mediaFileRepository.findAll();
    }

    private PersonInfo convertToPersonInfo(DetectedPerson person) {
        PersonInfo info = new PersonInfo();
        info.setId(person.getId());
        info.setUniqueId(person.getUniqueId());
        info.setAgeCategory(person.getAgeCategory());
        info.setEstimatedAge(person.getEstimatedAge());
        info.setGender(person.getGender());
        info.setConfidence(person.getConfidence());
        info.setEmotionalState(person.getEmotionalState());
        info.setFrameNumber(person.getFrameNumber());
        info.setTimestamp(person.getTimestamp());
        return info;
    }

    private ObjectInfo convertToObjectInfo(DetectedObject object) {
        ObjectInfo info = new ObjectInfo();
        info.setId(object.getId());
        info.setObjectName(object.getObjectName());
        info.setCategory(object.getCategory());
        info.setConfidence(object.getConfidence());
        info.setFrameNumber(object.getFrameNumber());
        info.setTimestamp(object.getTimestamp());
        return info;
    }

    private BookInfo convertToBookInfo(DetectedBook book) {
        BookInfo info = new BookInfo();
        info.setId(book.getId());
        info.setBookName(book.getBookName());
        info.setAuthor(book.getAuthor());
        info.setIsbn(book.getIsbn());
        info.setUniqueId(book.getUniqueId());
        info.setConfidence(book.getConfidence());
        info.setFrameNumber(book.getFrameNumber());
        info.setTimestamp(book.getTimestamp());
        return info;
    }

    private String generateSummary(int peopleCount, int objectsCount, int booksCount) {
        return String.format("Detected %d people, %d objects, and %d books in the media.",
                peopleCount, objectsCount, booksCount);
    }
}