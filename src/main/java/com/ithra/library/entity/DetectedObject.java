package com.ithra.library.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "detected_objects", indexes = {
        @Index(name = "idx_media_file", columnList = "media_file_id"),
        @Index(name = "idx_object_name", columnList = "objectName"),
        @Index(name = "idx_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectedObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    private String objectName;
    private String category;
    private Double confidence;
    private Integer frameNumber;
    private Double timestamp;

    // Position in frame
    private Double boundingBoxX;
    private Double boundingBoxY;
    private Double boundingBoxWidth;
    private Double boundingBoxHeight;

    @Column(columnDefinition = "TEXT")
    private String aiDescription;

    @CreationTimestamp
    private LocalDateTime detectedAt;
}
