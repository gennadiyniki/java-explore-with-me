package ru.practicum.ewm.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.stats.dto.ViewStatsDto;
import ru.practicum.ewm.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    //все посещения
    @Query("SELECT new ru.practicum.ewm.stats.dto.ViewStatsDto(" +
            "h.app, " +
            "h.uri, " +
            "COUNT(h.id)) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR h.uri IN :uris) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.id) DESC")
    List<ViewStatsDto> findStats(LocalDateTime start,
                                 LocalDateTime end,
                                 List<String> uris);

    //юник посещениям
    @Query("SELECT new ru.practicum.ewm.stats.dto.ViewStatsDto(" +
            "h.app, " +
            "h.uri, " +
            "COUNT(DISTINCT h.ip)) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR h.uri IN :uris) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStatsDto> findStatsUnique(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris);
}