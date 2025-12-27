package com.ithra.library.repository;

import com.ithra.library.entity.LiveStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {

    Optional<LiveStream> findByStreamKey(String streamKey);

    List<LiveStream> findByStatus(LiveStream.StreamStatus status);

    @Query("SELECT COUNT(s) FROM LiveStream s WHERE s.status = 'ACTIVE'")
    Long countActiveStreams();

    @Query("SELECT SUM(s.viewerCount) FROM LiveStream s WHERE s.status = 'ACTIVE'")
    Long sumActiveViewers();

    @Query("SELECT s FROM LiveStream s WHERE s.status IN ('ACTIVE', 'WAITING') " +
            "ORDER BY s.startTime DESC")
    List<LiveStream> findActiveStreams();
}
