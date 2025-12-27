package com.ithra.library.service;

import com.ithra.library.config.VideoProcessingService;
import com.ithra.library.dto.*;
import com.ithra.library.entity.DetectedBook;
import com.ithra.library.entity.DetectedObject;
import com.ithra.library.entity.DetectedPerson;
import com.ithra.library.entity.MediaFile;
import com.ithra.library.repository.DetectedBookRepository;
import com.ithra.library.repository.DetectedObjectRepository;
import com.ithra.library.repository.DetectedPersonRepository;
import com.ithra.library.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ithra.library.service.LiveStreamingService.*;

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
    private final OpenAIService aiService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Upload and analyze media file
     */
    @Transactional
    public MediaFile uploadFile(MultipartFile file) throws Exception {
        log.info("Uploading file: {}", file.getOriginalFilename());

        // Create upload directory
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Save file
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Determine file type
        String contentType = file.getContentType();
        MediaFile.FileType fileType = MediaFile.FileType.IMAGE;
        if (contentType != null && contentType.startsWith("video")) {
            fileType = MediaFile.FileType.VIDEO;
        }

        // Create media file entity
        MediaFile mediaFile = MediaFile.builder()
                .fileName(file.getOriginalFilename())
                .filePath(filePath.toString())
                .fileType(fileType)
                .fileSize(file.getSize())
                .status(MediaFile.ProcessingStatus.PROCESSING)
                .isLive(false)
                .build();

        mediaFile = mediaFileRepository.save(mediaFile);

        // Start async processing
        processMediaAsync(mediaFile.getId());

        return mediaFile;
    }

    /**
     * Process media file asynchronously
     */
    @Async
    @Transactional
    public void processMediaAsync(Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new RuntimeException("Media file not found"));

        try {
            log.info("Starting processing for file: {}", mediaFile.getFileName());

            if (mediaFile.getFileType() == MediaFile.FileType.IMAGE) {
                processImage(mediaFile);
            } else if (mediaFile.getFileType() == MediaFile.FileType.VIDEO) {
                processVideo(mediaFile);
            }

            // Generate AI summary
            MediaAnalysisResult analysis = getAnalysisResult(mediaFileId);
            String summary = aiService.generateMediaSummary(analysis);
            mediaFile.setAiSummary(summary);

            // Update counts
            mediaFile.setPeopleCount(analysis.getDetectedPeople().size());
            mediaFile.setObjectsCount(analysis.getDetectedObjects().size());
            mediaFile.setBooksCount(analysis.getDetectedBooks().size());

            mediaFile.setStatus(MediaFile.ProcessingStatus.COMPLETED);
            mediaFileRepository.save(mediaFile);

            log.info("Processing completed for file: {}", mediaFile.getFileName());

        } catch (Exception e) {
            log.error("Error processing media file", e);
            mediaFile.setStatus(MediaFile.ProcessingStatus.FAILED);
            mediaFile.setErrorMessage(e.getMessage());
            mediaFileRepository.save(mediaFile);
        }
    }

    private void processImage(MediaFile mediaFile) throws Exception {
        byte[] imageBytes = Files.readAllBytes(Paths.get(mediaFile.getFilePath()));

        CompletableFuture<FrameAnalysisResult> future =
                visionService.analyzeFrame(imageBytes, 0, 0.0);

        FrameAnalysisResult result = future.join();

        // Save detections
        saveDetections(result, mediaFile);

        mediaFile.setTotalFramesProcessed(1);
    }

    private void processVideo(MediaFile mediaFile) throws Exception {
        List<VideoProcessingService.VideoFrame> frames =
                videoService.extractFrames(mediaFile.getFilePath());

        int processedFrames = 0;

        for (VideoProcessingService.VideoFrame frame : frames) {
            CompletableFuture<FrameAnalysisResult> future =
                    visionService.analyzeFrame(
                            frame.getImageBytes(),
                            frame.getFrameNumber(),
                            frame.getTimestamp()
                    );

            FrameAnalysisResult result = future.join();
            saveDetections(result, mediaFile);
            processedFrames++;
        }

        mediaFile.setTotalFramesProcessed(processedFrames);
    }

    private void saveDetections(FrameAnalysisResult result, MediaFile mediaFile) {
        // Save people
        for (PersonInfo personInfo : result.getPeople()) {
            DetectedPerson person = convertToPersonEntity(personInfo, mediaFile);
            personRepository.save(person);
        }

        // Save objects
        for (ObjectInfo objectInfo : result.getObjects()) {
            DetectedObject object = convertToObjectEntity(objectInfo, mediaFile);
            objectRepository.save(object);
        }

        // Save books
        for (BookInfo bookInfo : result.getBooks()) {
            DetectedBook book = convertToBookEntity(bookInfo, mediaFile);
            bookRepository.save(book);
        }
    }

    /**
     * Get analysis result
     */
    @Transactional(readOnly = true)
    public MediaAnalysisResult getAnalysisResult(Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new RuntimeException("Media file not found"));

        List<DetectedPerson> people = personRepository.findByMediaFileId(mediaFileId);
        List<DetectedObject> objects = objectRepository.findByMediaFileId(mediaFileId);
        List<DetectedBook> books = bookRepository.findByMediaFileId(mediaFileId);

        return MediaAnalysisResult.builder()
                .mediaFileId(mediaFile.getId())
                .fileName(mediaFile.getFileName())
                .fileType(mediaFile.getFileType().name())
                .uploadDate(mediaFile.getUploadDate())
                .status(mediaFile.getStatus().name())
                .isLive(mediaFile.getIsLive())
                .streamUrl(mediaFile.getStreamUrl())
                .hlsPlaylistUrl(mediaFile.getHlsPlaylistUrl())
                .totalFramesProcessed(mediaFile.getTotalFramesProcessed())
                .duration(mediaFile.getDuration())
                .frameRate(mediaFile.getFrameRate())
                .detectedPeople(people.stream().map(LiveStreamingService::convertToPersonInfo).toList())
                .detectedObjects(objects.stream().map(LiveStreamingService::convertToObjectInfo).toList())
                .detectedBooks(books.stream().map(LiveStreamingService::convertToBookInfo).toList())
                .aiSummary(mediaFile.getAiSummary())
                .aiDescription(mediaFile.getAiDescription())
                .statistics(buildStatistics(people, objects, books))
                .build();
    }

    /**
     * List media files with pagination
     */
    public Page<MediaAnalysisResult> listMediaFiles(String fileType, String status,
                                                    Pageable pageable) {
        Page<MediaFile> mediaFiles;

        if (fileType != null && status != null) {
            mediaFiles = mediaFileRepository.findByFileTypeAndStatus(
                    MediaFile.FileType.valueOf(fileType),
                    MediaFile.ProcessingStatus.valueOf(status),
                    pageable
            );
        } else {
            mediaFiles = mediaFileRepository.findAll(pageable);
        }

        List<MediaAnalysisResult> results = mediaFiles.getContent().stream()
                .map(mf -> getAnalysisResult(mf.getId()))
                .collect(Collectors.toList());

        return new PageImpl<>(results, pageable, mediaFiles.getTotalElements());
    }

    /**
     * Delete media file
     */
    @Transactional
    public void deleteMediaFile(Long id) {
        MediaFile mediaFile = mediaFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media file not found"));

        // Delete file from filesystem
        try {
            Files.deleteIfExists(Paths.get(mediaFile.getFilePath()));
        } catch (Exception e) {
            log.error("Error deleting file from filesystem", e);
        }

        mediaFileRepository.delete(mediaFile);
    }

    /**
     * Get statistics for media file
     */
    public StatisticsInfo getStatistics(Long mediaFileId) {
        List<DetectedPerson> people = personRepository.findByMediaFileId(mediaFileId);
        List<DetectedObject> objects = objectRepository.findByMediaFileId(mediaFileId);
        List<DetectedBook> books = bookRepository.findByMediaFileId(mediaFileId);

        return buildStatistics(people, objects, books);
    }

    private StatisticsInfo buildStatistics(List<DetectedPerson> people,
                                           List<DetectedObject> objects,
                                           List<DetectedBook> books) {
        Map<String, Integer> peopleByAge = people.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getAgeCategory().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> peopleByEmotion = people.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getEmotionalState().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> objectsByCategory = objects.stream()
                .collect(Collectors.groupingBy(
                        DetectedObject::getCategory,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Double averageConfidence = Stream.concat(
                people.stream().map(DetectedPerson::getConfidence),
                objects.stream().map(DetectedObject::getConfidence)
        ).mapToDouble(Double::doubleValue).average().orElse(0.0);

        Integer uniquePeople = (int) people.stream()
                .map(DetectedPerson::getUniqueId)
                .distinct()
                .count();

        return StatisticsInfo.builder()
                .totalPeople(people.size())
                .totalObjects(objects.size())
                .totalBooks(books.size())
                .peopleByAge(peopleByAge)
                .peopleByEmotion(peopleByEmotion)
                .objectsByCategory(objectsByCategory)
                .averageConfidence(averageConfidence)
                .uniquePeople(uniquePeople)
                .build();
    }

}
