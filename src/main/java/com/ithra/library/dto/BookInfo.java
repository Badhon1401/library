
// BookInfo.java - Enhanced
package com.ithra.library.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookInfo {
    private Long id;
    private String bookName;
    private String author;
    private String isbn;
    private String publisher;
    private String publicationYear;
    private String uniqueId;
    private Double confidence;
    private Integer frameNumber;
    private Double timestamp;
    private String extractedText;
    private String aiSummary;
}
