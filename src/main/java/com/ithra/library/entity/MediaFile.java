// MediaFile.java - Enhanced with streaming support
package com.ithra.library.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "media_files", indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_file_type", columnList = "fileType"),
        @Index(name = "idx_upload_date", columnList = "uploadDate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String filePath;

    @Enumerated(EnumType.STRING)
    private FileType fileType; // IMAGE, VIDEO, LIVE_STREAM

    private Long fileSize;
    private Integer duration; // in seconds for videos
    private Integer width;
    private Integer height;
    private Double frameRate;

    @CreationTimestamp
    private LocalDateTime uploadDate;

    @UpdateTimestamp
    private LocalDateTime lastModified;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private String streamUrl; // For live streams
    private String hlsPlaylistUrl; // HLS playlist URL
    private Boolean isLive;
    private LocalDateTime streamStartTime;
    private LocalDateTime streamEndTime;

    @Column(columnDefinition = "TEXT")
    private String aiSummary; // AI-generated summary

    @Column(columnDefinition = "TEXT")
    private String aiDescription; // AI-generated description

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Integer totalFramesProcessed;
    private Integer peopleCount;
    private Integer objectsCount;
    private Integer booksCount;

    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetectedPerson> detectedPeople = new ArrayList<>();

    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetectedObject> detectedObjects = new ArrayList<>();

    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetectedBook> detectedBooks = new ArrayList<>();

    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<QueryHistory> queryHistories = new ArrayList<>();

    public enum FileType {
        IMAGE, VIDEO, LIVE_STREAM
    }

    public enum ProcessingStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, STREAMING
    }
}