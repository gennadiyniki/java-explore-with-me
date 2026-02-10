package ru.practicum.explorewithme.stats.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.stats.dto.EndpointHit;
import ru.practicum.explorewithme.stats.dto.ViewStats;
import ru.practicum.explorewithme.stats.server.service.StatServiceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public ResponseEntity<List<ViewStats>> getStats(@RequestParam String start,
                                                    @RequestParam String end,
                                                    @RequestParam(required = false) List<String> uris,
                                                    @RequestParam(defaultValue = "false") boolean unique) {
        log.info("=== STATS CONTROLLER: GET /stats ===");
        log.info("Raw params: start='{}', end='{}', uris={}, unique={}", start, end, uris, unique);

        try {
            // Декодируем пробелы если они закодированы
            start = start.replace("%20", " ");
            end = end.replace("%20", " ");

            log.info("Decoded params: start='{}', end='{}'", start, end);

            LocalDateTime startDate = LocalDateTime.parse(start, FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(end, FORMATTER);

            log.info("Parsed dates: start={}, end={}", startDate, endDate);

            if (startDate.isAfter(endDate)) {
                log.error("Invalid date range: start {} is after end {}", startDate, endDate);
                throw new IllegalArgumentException("Неверный диапазон дат: start не может быть после end");
            }

            List<ViewStats> stats = statServiceImpl.getStats(startDate, endDate, uris, unique);

            log.info("Returning {} stats records", stats.size());
            stats.forEach(s -> log.info("  -> {}: {} hits", s.getUri(), s.getHits()));

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error processing stats request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}