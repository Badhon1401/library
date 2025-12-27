package com.ithra.library.repository;

import com.ithra.library.entity.DetectedObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectedObjectRepository extends JpaRepository<DetectedObject, Long> {

    List<DetectedObject> findByMediaFileId(Long mediaFileId);

    List<DetectedObject> findByCategory(String category);

    @Query("SELECT o FROM DetectedObject o WHERE o.mediaFile.id = :mediaFileId " +
            "AND (LOWER(o.objectName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(o.category) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<DetectedObject> searchByKeyword(String keyword, Long mediaFileId);

    @Query("SELECT o FROM DetectedObject o WHERE o.mediaFile.id = :mediaFileId " +
            "AND o.timestamp BETWEEN :startTime AND :endTime")
    List<DetectedObject> findByMediaFileIdAndTimestampBetween(Long mediaFileId,
                                                              Double startTime,
                                                              Double endTime);

    @Query("SELECT COUNT(o) FROM DetectedObject o WHERE o.mediaFile.id = :mediaFileId")
    Long countByMediaFileId(Long mediaFileId);
}
