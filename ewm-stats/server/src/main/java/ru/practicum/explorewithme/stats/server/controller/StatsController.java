package ru.practicum.explorewithme.stats.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.stats.dto.EndpointHit;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;
import ru.practicum.explorewithme.stats.server.service.StatServiceImpl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class StatsController {
    private final StatServiceImpl statServiceImpl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/hit")
    public ResponseEntity<EndpointHit> hit(@Valid @RequestBody EndpointHit endpointHit) {
        log.info("=== STATS CONTROLLER: POST /hit ===");
        log.info("Received: {}", endpointHit);

        EndpointHit savedHit = statServiceImpl.saveHit(endpointHit);

        log.info("=== STATS CONTROLLER: Hit saved ===");
        return ResponseEntity.status(HttpStatus.CREATED).body(savedHit);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStatsDto>> getStats(@RequestParam String start,
                                                       @RequestParam String end,
                                                       @RequestParam(required = false) List<String> uris,
                                                       @RequestParam(defaultValue = "false") boolean unique) {
        log.info("=== STATS CONTROLLER: GET /stats ===");
        log.info("Raw params: start='{}', end='{}', uris={}, unique={}", start, end, uris, unique);

        try {
            // Декодирование URL
            start = URLDecoder.decode(start, StandardCharsets.UTF_8.toString());
            end = URLDecoder.decode(end, StandardCharsets.UTF_8.toString());

            log.info("Decoded params: start='{}', end='{}'", start, end);

            LocalDateTime startDate = LocalDateTime.parse(start, FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(end, FORMATTER);

            log.info("Parsed dates: start={}, end={}", startDate, endDate);

            if (startDate.isAfter(endDate)) {
                log.error("Invalid date range: start {} is after end {}", startDate, endDate);
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            List<ViewStatsDto> stats = statServiceImpl.getStats(startDate, endDate, uris, unique);

            log.info("Returning {} stats records", stats.size());
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error processing stats request: {}", e.getMessage(), e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/events")
    public ResponseEntity<List<ViewStatsDto>> getEventsStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {

        log.info("=== STATS CONTROLLER: GET /events ===");
        log.info("Params: start='{}', end='{}', uris={}, unique={}", start, end, uris, unique);

        try {
            // Декодирование URL
            start = URLDecoder.decode(start, StandardCharsets.UTF_8.toString());
            end = URLDecoder.decode(end, StandardCharsets.UTF_8.toString());

            log.info("Decoded params: start='{}', end='{}'", start, end);

            LocalDateTime startDate = LocalDateTime.parse(start, FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(end, FORMATTER);

            log.info("Parsed dates: start={}, end={}", startDate, endDate);

            if (startDate.isAfter(endDate)) {
                log.error("Invalid date range: start {} is after end {}", startDate, endDate);
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            List<ViewStatsDto> stats = statServiceImpl.getStats(startDate, endDate, uris, unique);

            // Сортировка по убыванию hits
            stats.sort((a, b) -> Long.compare(b.getHits(), a.getHits()));

            log.info("Returning {} event stats records", stats.size());
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error processing /events stats: {}", e.getMessage(), e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<List<ViewStatsDto>> getEventByIdStats(
            @PathVariable Long id,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(defaultValue = "false") boolean unique) {

        log.info("=== STATS CONTROLLER: GET /events/{} ===", id);
        log.info("Params: start='{}', end='{}', unique={}", start, end, unique);

        try {
            // Декодирование URL
            start = URLDecoder.decode(start, StandardCharsets.UTF_8.toString());
            end = URLDecoder.decode(end, StandardCharsets.UTF_8.toString());

            log.info("Decoded params: start='{}', end='{}'", start, end);

            LocalDateTime startDate = LocalDateTime.parse(start, FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(end, FORMATTER);

            log.info("Parsed dates: start={}, end={}", startDate, endDate);

            if (startDate.isAfter(endDate)) {
                log.error("Invalid date range: start {} is after end {}", startDate, endDate);
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            // Создаем URI для конкретного события
            String eventUri = "/events/" + id;
            List<String> uris = List.of(eventUri);

            List<ViewStatsDto> stats = statServiceImpl.getStats(startDate, endDate, uris, unique);

            log.info("Found {} stats records for event {}", stats.size(), id);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error processing /events/{} stats: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}