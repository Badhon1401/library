package com.ithra.library.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "detected_books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectedBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "media_file_id")
    private MediaFile mediaFile;

    private String bookName;
    private String author;
    private String isbn;
    private String uniqueId;
    private Double confidence;
    private Integer frameNumber;
    private Double timestamp;
}
