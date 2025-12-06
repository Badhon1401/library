package com.ithra.library.repository;

import com.ithra.library.entity.DetectedObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectedObjectRepository extends JpaRepository<DetectedObject, Long> {
    List<DetectedObject> findByMediaFileId(Long mediaFileId);

    List<DetectedObject> findByObjectNameContainingIgnoreCase(String objectName);

    @Query("SELECT do FROM DetectedObject do WHERE LOWER(do.objectName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND do.mediaFile.id = :mediaFileId")
    List<DetectedObject> searchByKeyword(@Param("keyword") String keyword, @Param("mediaFileId") Long mediaFileId);
}
