package com.ithra.library.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "detected_objects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectedObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "media_file_id")
    private MediaFile mediaFile;

    private String objectName;
    private String category;
    private Double confidence;
    private Integer frameNumber;
    private Double timestamp;
}
