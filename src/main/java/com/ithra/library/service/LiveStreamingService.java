// LiveStreamingService.java
package com.ithra.library.service;

import com.ithra.library.dto.*;
import com.ithra.library.entity.*;
import com.ithra.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveStreamingService {

    private final MediaFileRepository mediaFileRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final DetectedPersonRepository personRepository;
    private final DetectedObjectRepository objectRepository;
    private final DetectedBookRepository bookRepository;
    private final VisionAnalysisService visionService;
    private final OpenAIService aiService;

    @Value("${app.streaming.hls-dir}")
    private String hlsDir;

    @Value("${app.streaming.frame-extraction-interval}")
    private int frameInterval;

    private final Map<String, StreamProcessor> activeStreams = new ConcurrentHashMap<>();
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    /**
     * Start a new live stream
     */
    @Transactional
    public LiveStreamResponse startLiveStream(LiveStreamRequest request) {
        try {
            // Generate unique stream key
            String streamKey = UUID.randomUUID().toString();

            // Create media file
            MediaFile mediaFile = MediaFile.builder()
                    .fileName(request.getStreamName())
                    .fileType(MediaFile.FileType.LIVE_STREAM)
                    .status(MediaFile.ProcessingStatus.STREAMING)
                    .isLive(true)
                    .streamStartTime(LocalDateTime.now())
                    .build();

            mediaFile = mediaFileRepository.save(mediaFile);

            // Create live stream entity
            LiveStream liveStream = LiveStream.builder()
                    .mediaFile(mediaFile)
                    .streamKey(streamKey)
                    .rtmpUrl(generateRTMPUrl(streamKey))
                    .hlsUrl(generateHLSUrl(streamKey))
                    .status(LiveStream.StreamStatus.WAITING)
                    .startTime(LocalDateTime.now())
                    .viewerCount(0)
                    .build();

            liveStream = liveStreamRepository.save(liveStream);

            // Update media file with URLs
            mediaFile.setStreamUrl(liveStream.getRtmpUrl());
            mediaFile.setHlsPlaylistUrl(liveStream.getHlsUrl());
            mediaFileRepository.save(mediaFile);

            // Start stream processor
            startStreamProcessor(streamKey, mediaFile.getId(), request.getConfig());

            log.info("Live stream started: {}", streamKey);

            return LiveStreamResponse.builder()
                    .streamId(liveStream.getId())
                    .streamKey(streamKey)
                    .rtmpUrl(liveStream.getRtmpUrl())
                    .hlsUrl(liveStream.getHlsUrl())
                    .status(liveStream.getStatus().name())
                    .startTime(liveStream.getStartTime())
                    .viewerCount(0)
                    .build();

        } catch (Exception e) {
            log.error("Error starting live stream", e);
            throw new RuntimeException("Failed to start stream: " + e.getMessage());
        }
    }

    /**
     * Stop a live stream
     */
    @Transactional
    public void stopLiveStream(String streamKey) {
        StreamProcessor processor = activeStreams.remove(streamKey);
        if (processor != null) {
            processor.stop();
        }

        LiveStream liveStream = liveStreamRepository.findByStreamKey(streamKey)
                .orElseThrow(() -> new RuntimeException("Stream not found"));

        liveStream.setStatus(LiveStream.StreamStatus.ENDED);
        liveStream.setEndTime(LocalDateTime.now());
        liveStream.setDurationSeconds(
                java.time.Duration.between(liveStream.getStartTime(),
                        liveStream.getEndTime()).getSeconds()
        );

        MediaFile mediaFile = liveStream.getMediaFile();
        mediaFile.setIsLive(false);
        mediaFile.setStatus(MediaFile.ProcessingStatus.COMPLETED);
        mediaFile.setStreamEndTime(LocalDateTime.now());
        mediaFile.setDuration(liveStream.getDurationSeconds().intValue());

        // Generate AI summary for completed stream
        try {
            MediaAnalysisResult analysis = buildAnalysisResult(mediaFile);
            String summary = aiService.generateMediaSummary(analysis);
            mediaFile.setAiSummary(summary);
        } catch (Exception e) {
            log.error("Failed to generate AI summary", e);
        }

        liveStreamRepository.save(liveStream);
        mediaFileRepository.save(mediaFile);

        log.info("Live stream stopped: {}", streamKey);
    }

    /**
     * Get live stream status
     */
    public LiveStreamResponse getStreamStatus(String streamKey) {
        LiveStream liveStream = liveStreamRepository.findByStreamKey(streamKey)
                .orElseThrow(() -> new RuntimeException("Stream not found"));

        return LiveStreamResponse.builder()
                .streamId(liveStream.getId())
                .streamKey(streamKey)
                .rtmpUrl(liveStream.getRtmpUrl())
                .hlsUrl(liveStream.getHlsUrl())
                .status(liveStream.getStatus().name())
                .startTime(liveStream.getStartTime())
                .viewerCount(liveStream.getViewerCount())
                .build();
    }

    /**
     * List all active streams
     */
    public List<LiveStreamResponse> getActiveStreams() {
        return liveStreamRepository.findByStatus(LiveStream.StreamStatus.ACTIVE)
                .stream()
                .map(stream -> LiveStreamResponse.builder()
                        .streamId(stream.getId())
                        .streamKey(stream.getStreamKey())
                        .rtmpUrl(stream.getRtmpUrl())
                        .hlsUrl(stream.getHlsUrl())
                        .status(stream.getStatus().name())
                        .startTime(stream.getStartTime())
                        .viewerCount(stream.getViewerCount())
                        .build())
                .toList();
    }

    /**
     * Process incoming stream URL
     */
    @Async
    public void processStreamUrl(String streamUrl, Long mediaFileId) {
        String streamKey = "external-" + mediaFileId;
        startStreamProcessor(streamKey, mediaFileId, null);
    }

    // Private helper methods

    private void startStreamProcessor(String streamKey, Long mediaFileId,
                                      StreamConfig config) {
        StreamProcessor processor = new StreamProcessor(
                streamKey, mediaFileId, config
        );
        activeStreams.put(streamKey, processor);
        streamExecutor.submit(processor);
    }

    private String generateRTMPUrl(String streamKey) {
        return String.format("rtmp://localhost:1935/live/%s", streamKey);
    }

    private String generateHLSUrl(String streamKey) {
        return String.format("/hls/%s/playlist.m3u8", streamKey);
    }

    private MediaAnalysisResult buildAnalysisResult(MediaFile mediaFile) {
        List<DetectedPerson> people = personRepository.findByMediaFileId(mediaFile.getId());
        List<DetectedObject> objects = objectRepository.findByMediaFileId(mediaFile.getId());
        List<DetectedBook> books = bookRepository.findByMediaFileId(mediaFile.getId());

        return MediaAnalysisResult.builder()
                .mediaFileId(mediaFile.getId())
                .fileName(mediaFile.getFileName())
                .detectedPeople(people.stream().map(LiveStreamingService::convertToPersonInfo).toList())
                .detectedObjects(objects.stream().map(LiveStreamingService::convertToObjectInfo).toList())
                .detectedBooks(books.stream().map(LiveStreamingService::convertToBookInfo).toList())
                .build();
    }

    // Inner class for stream processing
    private class StreamProcessor implements Runnable {
        private final String streamKey;
        private final Long mediaFileId;
        private final StreamConfig config;
        private volatile boolean running = true;
        private FFmpegFrameGrabber grabber;

        public StreamProcessor(String streamKey, Long mediaFileId, StreamConfig config) {
            this.streamKey = streamKey;
            this.mediaFileId = mediaFileId;
            this.config = config;
        }

        @Override
        public void run() {
            try {
                // Initialize frame grabber for RTMP stream
                String rtmpUrl = generateRTMPUrl(streamKey);
                grabber = new FFmpegFrameGrabber(rtmpUrl);
                grabber.start();

                updateStreamStatus(LiveStream.StreamStatus.ACTIVE);

                Java2DFrameConverter converter = new Java2DFrameConverter();
                int frameCount = 0;
                double frameRate = grabber.getFrameRate();

                log.info("Stream processor started for {}", streamKey);

                while (running) {
                    Frame frame = grabber.grabImage();
                    if (frame == null) {
                        Thread.sleep(100);
                        continue;
                    }

                    frameCount++;

                    // Process frame at intervals
                    if (frameCount % frameInterval == 0) {
                        BufferedImage bufferedImage = converter.convert(frame);
                        if (bufferedImage != null) {
                            processFrame(bufferedImage, frameCount,
                                    frameCount / frameRate);
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Error processing stream", e);
                updateStreamStatus(LiveStream.StreamStatus.ERROR);
            } finally {
                cleanup();
            }
        }

        private void processFrame(BufferedImage image, int frameNumber,
                                  double timestamp) {
            try {
                // Convert to byte array
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                byte[] imageBytes = baos.toByteArray();

                // Analyze frame
                CompletableFuture<FrameAnalysisResult> future =
                        visionService.analyzeFrame(imageBytes, frameNumber, timestamp);

                future.thenAccept(result -> saveFrameAnalysis(result));

            } catch (Exception e) {
                log.error("Error processing frame {}", frameNumber, e);
            }
        }

        @Transactional
        private void saveFrameAnalysis(FrameAnalysisResult result) {
            MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                    .orElseThrow();

            // Save detected people
            for (PersonInfo personInfo : result.getPeople()) {
                DetectedPerson person = convertToPersonEntity(personInfo, mediaFile);
                personRepository.save(person);
            }

            // Save detected objects
            for (ObjectInfo objectInfo : result.getObjects()) {
                DetectedObject object = convertToObjectEntity(objectInfo, mediaFile);
                objectRepository.save(object);
            }

            // Save detected books
            for (BookInfo bookInfo : result.getBooks()) {
                DetectedBook book = convertToBookEntity(bookInfo, mediaFile);
                bookRepository.save(book);
            }

            // Update media file counts
            mediaFile.setTotalFramesProcessed(
                    (mediaFile.getTotalFramesProcessed() != null ?
                            mediaFile.getTotalFramesProcessed() : 0) + 1
            );
            mediaFileRepository.save(mediaFile);
        }

        private void updateStreamStatus(LiveStream.StreamStatus status) {
            liveStreamRepository.findByStreamKey(streamKey)
                    .ifPresent(stream -> {
                        stream.setStatus(status);
                        liveStreamRepository.save(stream);
                    });
        }

        public void stop() {
            running = false;
        }

        private void cleanup() {
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
            } catch (Exception e) {
                log.error("Error cleaning up grabber", e);
            }
        }
    }

    // Entity conversion methods
    public static PersonInfo convertToPersonInfo(DetectedPerson person) {
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

    public static ObjectInfo convertToObjectInfo(DetectedObject object) {
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

    public static BookInfo convertToBookInfo(DetectedBook book) {
        return BookInfo.builder()
                .id(book.getId())
                .bookName(book.getBookName())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .uniqueId(book.getUniqueId())
                .confidence(book.getConfidence())
                .frameNumber(book.getFrameNumber())
                .timestamp(book.getTimestamp())
                .aiSummary(book.getAiSummary())
                .build();
    }

    public static DetectedPerson convertToPersonEntity(PersonInfo info, MediaFile mediaFile) {
        return DetectedPerson.builder()
                .mediaFile(mediaFile)
                .uniqueId(info.getUniqueId())
                .ageCategory(DetectedPerson.AgeCategory.valueOf(info.getAgeCategory()))
                .estimatedAge(info.getEstimatedAge())
                .gender(DetectedPerson.Gender.valueOf(info.getGender()))
                .confidence(info.getConfidence())
                .emotionalState(DetectedPerson.EmotionalState.valueOf(info.getEmotionalState()))
                .frameNumber(info.getFrameNumber())
                .timestamp(info.getTimestamp())
                .aiDescription(info.getAiDescription())
                .build();
    }

    public static DetectedObject convertToObjectEntity(ObjectInfo info, MediaFile mediaFile) {
        return DetectedObject.builder()
                .mediaFile(mediaFile)
                .objectName(info.getObjectName())
                .category(info.getCategory())
                .confidence(info.getConfidence())
                .frameNumber(info.getFrameNumber())
                .timestamp(info.getTimestamp())
                .aiDescription(info.getAiDescription())
                .build();
    }

    public static DetectedBook convertToBookEntity(BookInfo info, MediaFile mediaFile) {
        return DetectedBook.builder()
                .mediaFile(mediaFile)
                .bookName(info.getBookName())
                .author(info.getAuthor())
                .isbn(info.getIsbn())
                .uniqueId(info.getUniqueId())
                .confidence(info.getConfidence())
                .frameNumber(info.getFrameNumber())
                .timestamp(info.getTimestamp())
                .aiSummary(info.getAiSummary())
                .build();
    }
}