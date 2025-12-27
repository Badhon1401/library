// DetectedBook.java - Enhanced
package com.ithra.library.entity;

import jakarta.persistence.*;
        import lombok.*;
        import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "detected_books", indexes = {
        @Index(name = "idx_media_file", columnList = "media_file_id"),
        @Index(name = "idx_isbn", columnList = "isbn"),
        @Index(name = "idx_book_name", columnList = "bookName")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectedBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    private String bookName;
    private String author;
    private String isbn;
    private String publisher;
    private String publicationYear;
    private String uniqueId;
    private Double confidence;
    private Integer frameNumber;
    private Double timestamp;

    @Column(columnDefinition = "TEXT")
    private String extractedText; // Full OCR text

    @Column(columnDefinition = "TEXT")
    private String aiSummary; // AI summary of book content

    @CreationTimestamp
    private LocalDateTime detectedAt;
}

