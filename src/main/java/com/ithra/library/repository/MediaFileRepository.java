package com.ithra.library.repository;

import com.ithra.library.entity.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByFileType(MediaFile.FileType fileType);

    List<MediaFile> findByStatus(MediaFile.ProcessingStatus status);

    List<MediaFile> findByIsLiveTrue();

    Page<MediaFile> findByFileTypeAndStatus(MediaFile.FileType fileType,
                                            MediaFile.ProcessingStatus status,
                                            Pageable pageable);

    @Query("SELECT m FROM MediaFile m WHERE m.uploadDate BETWEEN :start AND :end")
    List<MediaFile> findByUploadDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(m) FROM MediaFile m WHERE m.isLive = true")
    Long countActiveLiveStreams();

    @Query("SELECT m FROM MediaFile m ORDER BY m.uploadDate DESC")
    List<MediaFile> findRecentMediaFiles(Pageable pageable);
}
