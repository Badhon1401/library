package com.ithra.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookInfo {
    private Long id;
    private String bookName;
    private String author;
    private String isbn;
    private String uniqueId;
    private Double confidence;
    private Integer frameNumber;
    private Double timestamp;
}
