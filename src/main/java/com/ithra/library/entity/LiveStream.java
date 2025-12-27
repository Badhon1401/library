package com.ithra.library.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_streams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveStream {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "media_file_id")
    private MediaFile mediaFile;

    private String streamKey;
    private String rtmpUrl;
    private String hlsUrl;
    private String webrtcUrl;

    @Enumerated(EnumType.STRING)
    private StreamStatus status;

    private Integer viewerCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationSeconds;

    @Column(columnDefinition = "TEXT")
    private String streamMetadata; // JSON metadata

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum StreamStatus {
        WAITING, ACTIVE, PAUSED, ENDED, ERROR
    }
}
