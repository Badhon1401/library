package com.ithra.library.repository;

import com.ithra.library.entity.DetectedPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectedPersonRepository extends JpaRepository<DetectedPerson, Long> {
    List<DetectedPerson> findByMediaFileId(Long mediaFileId);

    List<DetectedPerson> findByAgeCategory(String ageCategory);

    @Query("SELECT dp FROM DetectedPerson dp WHERE dp.mediaFile.id = :mediaFileId")
    List<DetectedPerson> findAllByMediaFile(@Param("mediaFileId") Long mediaFileId);
}
