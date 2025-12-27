package com.ithra.library.repository;

import com.ithra.library.entity.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {

    List<QueryHistory> findByMediaFileIdOrderByQueryTimeDesc(Long mediaFileId);

    @Query("SELECT q.query, COUNT(q) as count FROM QueryHistory q " +
            "GROUP BY q.query ORDER BY count DESC")
    List<Object[]> findTopQueries();

    @Query("SELECT q FROM QueryHistory q WHERE q.queryTime BETWEEN :start AND :end")
    List<QueryHistory> findByQueryTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT AVG(q.responseTime) FROM QueryHistory q")
    Double findAverageResponseTime();
}
