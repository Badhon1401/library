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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

    @Value("${app.thumbnail.dir:uploads/thumbnails}")
    private String thumbnailDir;

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

            // Generate thumbnail
            generateThumbnailAsync(mediaFile);

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

            // Delete thumbnail if exists
            Path thumbnailPath = getThumbnailPath(mediaFile);
            Files.deleteIfExists(thumbnailPath);
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

    /**
     * Get media file by ID
     */
    public MediaFile getMediaFileById(Long id) {
        return mediaFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media file not found with id: " + id));
    }

    /**
     * Get thumbnail for media file
     */
    public Resource getThumbnail(Long mediaFileId) {
        try {
            MediaFile mediaFile = getMediaFileById(mediaFileId);
            Path thumbnailPath = getThumbnailPath(mediaFile);

            if (!Files.exists(thumbnailPath)) {
                log.info("Thumbnail not found, generating for media file: {}", mediaFileId);
                generateThumbnail(mediaFile, thumbnailPath);
            }

            Resource resource = new UrlResource(thumbnailPath.toUri());

            if (resource.exists()) {
                return resource;
            }

            return null;

        } catch (Exception e) {
            log.error("Error getting thumbnail", e);
            return null;
        }
    }

    /**
     * Generate thumbnail asynchronously
     */
    @Async
    public void generateThumbnailAsync(MediaFile mediaFile) {
        try {
            Path thumbnailPath = getThumbnailPath(mediaFile);
            if (!Files.exists(thumbnailPath)) {
                generateThumbnail(mediaFile, thumbnailPath);
                log.info("Thumbnail generated for media file: {}", mediaFile.getId());
            }
        } catch (Exception e) {
            log.error("Error generating thumbnail asynchronously", e);
        }
    }

    /**
     * Get thumbnail path
     */
    private Path getThumbnailPath(MediaFile mediaFile) {
        String thumbnailName = "thumb_" + mediaFile.getId() + ".jpg";
        return Paths.get(thumbnailDir, thumbnailName);
    }

    /**
     * Generate thumbnail
     */
    private void generateThumbnail(MediaFile mediaFile, Path thumbnailPath) throws IOException {
        // Create thumbnails directory if it doesn't exist
        Files.createDirectories(thumbnailPath.getParent());

        if (mediaFile.getFileType() == MediaFile.FileType.IMAGE) {
            generateImageThumbnail(mediaFile.getFilePath(), thumbnailPath);
        } else if (mediaFile.getFileType() == MediaFile.FileType.VIDEO) {
            generateVideoThumbnail(mediaFile.getFilePath(), thumbnailPath);
        }
    }

    /**
     * Generate image thumbnail
     */
    private void generateImageThumbnail(String imagePath, Path thumbnailPath) throws IOException {
        BufferedImage original = ImageIO.read(new File(imagePath));

        if (original == null) {
            throw new IOException("Unable to read image file: " + imagePath);
        }

        int targetWidth = 400;
        int targetHeight = (int) (original.getHeight() * (targetWidth / (double) original.getWidth()));

        BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        ImageIO.write(thumbnail, "jpg", thumbnailPath.toFile());
        log.info("Image thumbnail generated: {}", thumbnailPath);
    }

    /**
     * Generate video thumbnail using FFmpeg
     */
    private void generateVideoThumbnail(String videoPath, Path thumbnailPath) throws IOException {
        try {
            // Using FFmpeg command (make sure FFmpeg is installed)
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", videoPath,
                    "-ss", "00:00:01",
                    "-vframes", "1",
                    "-vf", "scale=400:-1",
                    "-y",
                    thumbnailPath.toString()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.warn("FFmpeg exited with code: {}", exitCode);
                throw new IOException("Failed to generate video thumbnail");
            }

            log.info("Video thumbnail generated: {}", thumbnailPath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thumbnail generation interrupted", e);
        } catch (IOException e) {
            log.error("Error running FFmpeg. Make sure FFmpeg is installed.", e);
            throw e;
        }
    }

    /**
     * Build statistics
     */
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

    @Async
    @Transactional
    public void generateAllMissingThumbnails() {
        log.info("Starting batch thumbnail generation for all media files...");

        List<MediaFile> allMediaFiles = mediaFileRepository.findAll();
        int generated = 0;
        int skipped = 0;
        int failed = 0;

        for (MediaFile mediaFile : allMediaFiles) {
            try {
                Path thumbnailPath = getThumbnailPath(mediaFile);

                if (Files.exists(thumbnailPath)) {
                    log.debug("Thumbnail already exists for media file: {}", mediaFile.getId());
                    skipped++;
                    continue;
                }

                log.info("Generating thumbnail for media file: {} - {}",
                        mediaFile.getId(), mediaFile.getFileName());

                generateThumbnail(mediaFile, thumbnailPath);
                generated++;

            } catch (Exception e) {
                log.error("Failed to generate thumbnail for media file: {}", mediaFile.getId(), e);
                failed++;
            }
        }

        log.info("Batch thumbnail generation completed. Generated: {}, Skipped: {}, Failed: {}",
                generated, skipped, failed);
    }
}