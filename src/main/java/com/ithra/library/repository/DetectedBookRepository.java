package com.ithra.library.repository;

import com.ithra.library.entity.DetectedBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectedBookRepository extends JpaRepository<DetectedBook, Long> {
    List<DetectedBook> findByMediaFileId(Long mediaFileId);

    List<DetectedBook> findByBookNameContainingIgnoreCase(String bookName);
}
