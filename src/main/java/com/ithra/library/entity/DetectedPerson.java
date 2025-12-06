package com.ithra.library.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "detected_persons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectedPerson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "media_file_id")
    private MediaFile mediaFile;

    private String uniqueId;
    private String ageCategory; // CHILD, TEEN, ADULT, SENIOR
    private Integer estimatedAge;
    private String gender;
    private Double confidence;
    private String emotionalState;
    private Integer frameNumber; // For videos
    private Double timestamp; // For videos
}
