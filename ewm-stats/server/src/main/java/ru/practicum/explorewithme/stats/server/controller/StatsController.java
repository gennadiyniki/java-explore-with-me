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
import ru.practicum.explorewithme.stats.server.service.StatService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class StatsController {
    private final StatService statService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/hit")
    public ResponseEntity<EndpointHit> hit(@Valid @RequestBody EndpointHit endpointHit) {
        log.info("=== POST /hit ===");
        log.info("Received hit: {}", endpointHit);

        try {
            EndpointHit savedHit = statService.saveHit(endpointHit);
            log.info("Hit saved successfully: {}", savedHit);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedHit);
        } catch (Exception e) {
            log.error("Error saving hit: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStatsDto>> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {

        log.info("=== GET /stats ===");
        log.info("RAW PARAMETERS:");
        log.info("  start (raw): '{}'", start);
        log.info("  end (raw): '{}'", end);
        log.info("  uris (raw): {}", uris);
        log.info("  unique (raw): {}", unique);

        try {
            // Декодируем URL-encoded параметры
            String decodedStart = URLDecoder.decode(start, StandardCharsets.UTF_8);
            String decodedEnd = URLDecoder.decode(end, StandardCharsets.UTF_8);

            log.info("DECODED PARAMETERS:");
            log.info("  start (decoded): '{}'", decodedStart);
            log.info("  end (decoded): '{}'", decodedEnd);

            // Парсим даты
            LocalDateTime startDate;
            LocalDateTime endDate;

            try {
                startDate = LocalDateTime.parse(decodedStart, FORMATTER);
                endDate = LocalDateTime.parse(decodedEnd, FORMATTER);
            } catch (DateTimeParseException e) {
                log.error("ERROR PARSING DATES:");
                log.error("  Failed to parse start: '{}'", decodedStart);
                log.error("  Failed to parse end: '{}'", decodedEnd);
                log.error("  Exception: {}", e.getMessage());
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            log.info("PARSED DATES:");
            log.info("  start (parsed): {}", startDate);
            log.info("  end (parsed): {}", endDate);

            if (startDate.isAfter(endDate)) {
                log.error("Invalid date range: start {} is after end {}", startDate, endDate);
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            // Получаем статистику
            List<ViewStatsDto> stats = statService.getStats(startDate, endDate, uris, unique);

            log.info("RETURNING {} STATS RECORDS:", stats.size());
            for (ViewStatsDto stat : stats) {
                log.info("  {} - {}: {} hits",
                        stat.getApp(), stat.getUri(), stat.getHits());
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("UNEXPECTED ERROR processing /stats request:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
}