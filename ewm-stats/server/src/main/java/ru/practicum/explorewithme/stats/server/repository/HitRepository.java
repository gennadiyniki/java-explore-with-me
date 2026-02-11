package ru.practicum.explorewithme.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;
import ru.practicum.explorewithme.stats.server.entity.Hit;

import java.time.LocalDateTime;
import java.util.List;

public interface HitRepository extends JpaRepository<Hit, Long> {


    @Query("SELECT new ru.practicum.explorewithme.stats.dto.ViewStatsDto(" +
            "h.app, h.uri, COUNT(h.ip)) " +  // COUNT(h.ip) а не COUNT(h)!
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR h.uri IN :uris) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.ip) DESC")
    List<ViewStatsDto> findStats(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 @Param("uris") List<String> uris);

    @Query("SELECT new ru.practicum.explorewithme.stats.dto.ViewStatsDto(" +
            "h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR h.uri IN :uris) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStatsDto> findUniqueStats(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("uris") List<String> uris);

    // Дополнительные методы для отладки
    List<Hit> findByUri(String uri);

    List<Hit> findByUriAndTimestampBetween(String uri, LocalDateTime start, LocalDateTime end);

    long countByUri(String uri);

    List<Hit> findAllByUriAndTimestampBetween(String path, LocalDateTime of, LocalDateTime of1);
}