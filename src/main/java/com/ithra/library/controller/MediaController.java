package com.ithra.library.controller;

import com.ithra.library.dto.*;
import com.ithra.library.entity.MediaFile;
import com.ithra.library.service.MediaAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MediaController {

    private final MediaAnalysisService mediaAnalysisService;

    /**
     * Upload and analyze media file
     */
    @PostMapping("/upload")
    public ResponseEntity<MediaAnalysisResult> uploadFile(
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            MediaFile mediaFile = mediaAnalysisService.uploadFile(file);
            MediaAnalysisResult result = mediaAnalysisService
                    .getAnalysisResult(mediaFile.getId());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get analysis result by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MediaAnalysisResult> getAnalysisResult(@PathVariable Long id) {
        try {
            MediaAnalysisResult result = mediaAnalysisService.getAnalysisResult(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting analysis result", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List all media files with pagination
     */
    @GetMapping
    public ResponseEntity<Page<MediaAnalysisResult>> listMediaFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String status) {
        try {
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by("uploadDate").descending());

            Page<MediaAnalysisResult> results = mediaAnalysisService
                    .listMediaFiles(fileType, status, pageable);

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error listing media files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete media file
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMediaFile(@PathVariable Long id) {
        try {
            mediaAnalysisService.deleteMediaFile(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting media file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get media statistics
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<StatisticsInfo> getStatistics(@PathVariable Long id) {
        try {
            StatisticsInfo stats = mediaAnalysisService.getStatistics(id);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting statistics", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Serve original media file
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getMediaFile(@PathVariable Long id) {
        try {
            MediaFile mediaFile = mediaAnalysisService.getMediaFileById(id);
            Path filePath = Paths.get(mediaFile.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                log.warn("Media file not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = mediaFile.getFileType() == MediaFile.FileType.VIDEO
                    ? "video/mp4"
                    : "image/jpeg";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + mediaFile.getFileName() + "\"")
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                    .body(resource);

        } catch (Exception e) {
            log.error("Error serving media file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Serve thumbnail with automatic generation and fallback
     */
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable Long id) {
        try {
            log.info("Serving thumbnail for media file: {}", id);

            // Try to get thumbnail from service
            Resource thumbnailResource = mediaAnalysisService.getThumbnail(id);

            if (thumbnailResource != null && thumbnailResource.exists()) {
                log.info("Thumbnail found and served for media file: {}", id);
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(thumbnailResource);
            }

            // If thumbnail doesn't exist, try to generate it on-the-fly
            log.info("Thumbnail not found, attempting to generate for media file: {}", id);
            MediaFile mediaFile = mediaAnalysisService.getMediaFileById(id);

            // Generate thumbnail synchronously for immediate response
            Resource generatedThumbnail = generateThumbnailOnTheFly(mediaFile);

            if (generatedThumbnail != null && generatedThumbnail.exists()) {
                log.info("Thumbnail generated on-the-fly for media file: {}", id);
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(generatedThumbnail);
            }

            // Last fallback: serve the original file (scaled down by browser)
            log.warn("Could not generate thumbnail, falling back to original file for media file: {}", id);
            return getMediaFile(id);

        } catch (Exception e) {
            log.error("Error serving thumbnail for media file: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate thumbnail on-the-fly if it doesn't exist
     */
    private Resource generateThumbnailOnTheFly(MediaFile mediaFile) {
        try {
            // Create thumbnails directory if it doesn't exist
            String thumbnailDir = System.getProperty("user.dir") + "/uploads/thumbnails";
            Path thumbnailDirPath = Paths.get(thumbnailDir);
            Files.createDirectories(thumbnailDirPath);

            String thumbnailName = "thumb_" + mediaFile.getId() + ".jpg";
            Path thumbnailPath = thumbnailDirPath.resolve(thumbnailName);

            // If already exists, return it
            if (Files.exists(thumbnailPath)) {
                return new UrlResource(thumbnailPath.toUri());
            }

            // Generate based on file type
            if (mediaFile.getFileType() == MediaFile.FileType.IMAGE) {
                generateImageThumbnail(mediaFile.getFilePath(), thumbnailPath);
            } else if (mediaFile.getFileType() == MediaFile.FileType.VIDEO) {
                generateVideoThumbnail(mediaFile.getFilePath(), thumbnailPath);
            }

            return new UrlResource(thumbnailPath.toUri());

        } catch (Exception e) {
            log.error("Error generating thumbnail on-the-fly", e);
            return null;
        }
    }

    /**
     * Generate image thumbnail
     */
    private void generateImageThumbnail(String imagePath, Path thumbnailPath) throws Exception {
        BufferedImage original = ImageIO.read(new File(imagePath));

        if (original == null) {
            throw new Exception("Unable to read image file: " + imagePath);
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
    private void generateVideoThumbnail(String videoPath, Path thumbnailPath) throws Exception {
        try {
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
                throw new Exception("Failed to generate video thumbnail");
            }

            log.info("Video thumbnail generated: {}", thumbnailPath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Thumbnail generation interrupted", e);
        } catch (Exception e) {
            log.error("Error running FFmpeg. Make sure FFmpeg is installed.", e);
            throw e;
        }
    }

    /**
     * Generate all missing thumbnails (utility endpoint)
     */
    @PostMapping("/thumbnails/generate-all")
    public ResponseEntity<String> generateAllThumbnails() {
        try {
            log.info("Starting batch thumbnail generation...");
            mediaAnalysisService.generateAllMissingThumbnails();
            return ResponseEntity.ok("Thumbnail generation started for all media files");
        } catch (Exception e) {
            log.error("Error generating thumbnails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}