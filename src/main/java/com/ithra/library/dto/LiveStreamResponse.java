package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveStreamResponse {
    private Long streamId;
    private String streamKey;
    private String rtmpUrl;
    private String hlsUrl;
    private String webrtcUrl;
    private String status;
    private LocalDateTime startTime;
    private Integer viewerCount;
}
