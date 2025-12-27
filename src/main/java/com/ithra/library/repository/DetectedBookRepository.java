// MediaFileRepository.java
package com.ithra.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;



import com.ithra.library.entity.DetectedBook;

import java.util.Optional;

@Repository
public interface DetectedBookRepository extends JpaRepository<DetectedBook, Long> {

    List<DetectedBook> findByMediaFileId(Long mediaFileId);

    Optional<DetectedBook> findByIsbn(String isbn);

    List<DetectedBook> findByAuthor(String author);

    @Query("SELECT b FROM DetectedBook b WHERE b.mediaFile.id = :mediaFileId " +
            "AND (LOWER(b.bookName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<DetectedBook> searchByKeyword(String keyword, Long mediaFileId);

    @Query("SELECT COUNT(b) FROM DetectedBook b WHERE b.mediaFile.id = :mediaFileId")
    Long countByMediaFileId(Long mediaFileId);
}
