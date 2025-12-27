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
public class RecentActivity {
    private String type;
    private String description;
    private LocalDateTime timestamp;
    private Long mediaFileId;
}
