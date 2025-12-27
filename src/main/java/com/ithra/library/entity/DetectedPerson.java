package com.ithra.library.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "detected_persons", indexes = {
        @Index(name = "idx_media_file", columnList = "media_file_id"),
        @Index(name = "idx_unique_id", columnList = "uniqueId"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectedPerson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    private String uniqueId; // Track same person across frames

    @Enumerated(EnumType.STRING)
    private AgeCategory ageCategory;

    private Integer estimatedAge;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Double confidence;

    @Enumerated(EnumType.STRING)
    private EmotionalState emotionalState;

    private Integer frameNumber;
    private Double timestamp;

    // Facial landmarks
    private String facialLandmarks; // JSON string

    // Position in frame
    private Double boundingBoxX;
    private Double boundingBoxY;
    private Double boundingBoxWidth;
    private Double boundingBoxHeight;

    @Column(columnDefinition = "TEXT")
    private String aiDescription; // AI-generated description

    @CreationTimestamp
    private LocalDateTime detectedAt;

    public enum AgeCategory {
        CHILD, TEEN, ADULT, SENIOR
    }

    public enum Gender {
        MALE, FEMALE, UNKNOWN
    }

    public enum EmotionalState {
        HAPPY, SAD, ANGRY, SURPRISED, FEARFUL, DISGUSTED, NEUTRAL
    }
}
