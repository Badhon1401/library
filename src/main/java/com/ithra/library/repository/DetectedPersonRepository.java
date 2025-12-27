package com.ithra.library.repository;

import com.ithra.library.entity.DetectedPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectedPersonRepository extends JpaRepository<DetectedPerson, Long> {

    List<DetectedPerson> findByMediaFileId(Long mediaFileId);

    List<DetectedPerson> findByUniqueId(String uniqueId);

    List<DetectedPerson> findByAgeCategory(DetectedPerson.AgeCategory ageCategory);

    List<DetectedPerson> findByEmotionalState(DetectedPerson.EmotionalState emotionalState);

    @Query("SELECT p FROM DetectedPerson p WHERE p.mediaFile.id = :mediaFileId " +
            "AND p.timestamp BETWEEN :startTime AND :endTime")
    List<DetectedPerson> findByMediaFileIdAndTimestampBetween(Long mediaFileId,
                                                              Double startTime,
                                                              Double endTime);

    @Query("SELECT COUNT(p) FROM DetectedPerson p WHERE p.mediaFile.id = :mediaFileId")
    Long countByMediaFileId(Long mediaFileId);

    @Query("SELECT COUNT(DISTINCT p.uniqueId) FROM DetectedPerson p " +
            "WHERE p.mediaFile.id = :mediaFileId")
    Long countUniquePersonsByMediaFileId(Long mediaFileId);
}
