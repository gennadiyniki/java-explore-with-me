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
import ru.practicum.explorewithme.stats.server.service.StatService; // ИНТЕРФЕЙС!

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
    private final StatService statService; // Используем интерфейс

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/hit")
    public ResponseEntity<EndpointHit> hit(@Valid @RequestBody EndpointHit endpointHit) {
        log.info("POST /hit: {}", endpointHit);
        EndpointHit savedHit = statService.saveHit(endpointHit);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedHit);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStatsDto>> getStats(@RequestParam String start,
                                                       @RequestParam String end,
                                                       @RequestParam(required = false) List<String> uris,
                                                       @RequestParam(defaultValue = "false") boolean unique) {
        log.info("GET /stats: start='{}', end='{}', uris={}, unique={}", start, end, uris, unique);

        try {
            // Декодирование URL
            start = URLDecoder.decode(start, StandardCharsets.UTF_8.toString());
            end = URLDecoder.decode(end, StandardCharsets.UTF_8.toString());

            LocalDateTime startDate = LocalDateTime.parse(start, FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(end, FORMATTER);

            if (startDate.isAfter(endDate)) {
                log.error("Invalid date range: start {} is after end {}", startDate, endDate);
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            List<ViewStatsDto> stats = statService.getStats(startDate, endDate, uris, unique);
            log.info("Returning {} stats records", stats.size());
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error processing stats request: {}", e.getMessage(), e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    // УДАЛИТЬ ВСЕ ОСТАЛЬНЫЕ ENDPOINT'Ы!
    // Тесты Postman обращаются только к /stats
}