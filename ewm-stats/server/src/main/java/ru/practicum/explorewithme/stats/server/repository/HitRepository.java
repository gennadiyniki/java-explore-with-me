package ru.practicum.explorewithme.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.stats.dto.ViewStats;
import ru.practicum.explorewithme.stats.server.entity.Hit;

import java.time.LocalDateTime;
import java.util.List;

// Репозиторий для работы со статистикой
public interface HitRepository extends JpaRepository<Hit, Long> {

    @Query("SELECT new ru.practicum.explorewithme.stats.dto.ViewStats(h.app, h.uri, COUNT(h)) " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h) DESC")
    List<ViewStats> findAllStats(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.explorewithme.stats.dto.ViewStats(h.app, h.uri, COUNT(h)) " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND h.uri IN :uris " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h) DESC")
    List<ViewStats> findStatsByUris(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("uris") List<String> uris);

    // Найти по URI в период времени
    List<Hit> findAllByUriAndTimestampBetween(String uri, LocalDateTime start, LocalDateTime end);

    // Найти все в период времени
    List<Hit> findAllByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Найти уникальные по URI, APP и IP в период времени
    @Query("SELECT h FROM Hit h WHERE h.timestamp BETWEEN :start AND :end GROUP BY h.uri, h.app, h.ip")
    List<Hit> findDistinctByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}