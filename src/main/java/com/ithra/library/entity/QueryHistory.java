package com.ithra.library.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "query_history", indexes = {
        @Index(name = "idx_media_file", columnList = "media_file_id"),
        @Index(name = "idx_query_time", columnList = "queryTime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id")
    private MediaFile mediaFile;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String query;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String aiResponse; // Full AI response

    private Integer matchCount;
    private Double responseTime; // in milliseconds

    @CreationTimestamp
    private LocalDateTime queryTime;
}
