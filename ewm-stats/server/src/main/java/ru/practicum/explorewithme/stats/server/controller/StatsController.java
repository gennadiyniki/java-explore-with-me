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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.explorewithme.stats.dto.Constants.FORMATTER;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class StatsController {
    private final StatServiceImpl statServiceImpl;

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // Сначала декодируем URL
            String decoded = URLDecoder.decode(dateTimeStr, StandardCharsets.UTF_8.toString());

            // Пробуем ISO формат (с 'T')
            try {
                return LocalDateTime.parse(decoded, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e1) {
                // Пробуем формат с пробелом (старый)
                try {
                    return LocalDateTime.parse(decoded, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (DateTimeParseException e2) {
                    // Пробуем без секунд
                    try {
                        return LocalDateTime.parse(decoded, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    } catch (DateTimeParseException e3) {
                        throw new IllegalArgumentException("Неверный формат даты: " + decoded);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка обработки даты: " + dateTimeStr, e);
        }
    }

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
            // Полное URL декодирование
            start = URLDecoder.decode(start, StandardCharsets.UTF_8.toString());
            end = URLDecoder.decode(end, StandardCharsets.UTF_8.toString());

            log.info("Decoded params: start='{}', end='{}'", start, end);

            LocalDateTime startDate = LocalDateTime.parse(start, DateTimeFormatter.ofPattern(FORMATTER));
            LocalDateTime endDate = LocalDateTime.parse(end, DateTimeFormatter.ofPattern(FORMATTER));

            log.info("Parsed dates: start={}, end={}", startDate, endDate);

            if (startDate.isAfter(endDate)) {
                log.error("Invalid date range: start {} is after end {}", startDate, endDate);
                throw new IllegalArgumentException("Неверный диапазон дат: start не может быть после end");
            }

            List<ViewStatsDto> stats = statServiceImpl.getStats(startDate, endDate, uris, unique);

            log.info("Returning {} stats records", stats.size());
            stats.forEach(s -> log.info("  -> {}: {} hits", s.getUri(), s.getHits()));

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error processing stats request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
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
            // Полное URL декодирование
            start = URLDecoder.decode(start, StandardCharsets.UTF_8.toString());
            end = URLDecoder.decode(end, StandardCharsets.UTF_8.toString());

            log.info("Decoded params: start='{}', end='{}'", start, end);

            LocalDateTime startDate = LocalDateTime.parse(start, DateTimeFormatter.ofPattern(FORMATTER));
            LocalDateTime endDate = LocalDateTime.parse(end, DateTimeFormatter.ofPattern(FORMATTER));

            log.info("Parsed dates: start={}, end={}", startDate, endDate);

            if (startDate.isAfter(endDate)) {
                log.error("Invalid date range: start {} is after end {}", startDate, endDate);
                throw new IllegalArgumentException("Неверный диапазон дат: start не может быть после end");
            }

            List<ViewStatsDto> stats;
            if (uris == null || uris.isEmpty()) {
                // Получаем всю статистику
                stats = statServiceImpl.getStats(startDate, endDate, null, unique);
                // Фильтруем только события (URI начинаются с /events)
                stats = stats.stream()
                        .filter(stat -> stat.getUri() != null && stat.getUri().startsWith("/events"))
                        .collect(Collectors.toList());
            } else {
                // Получаем только указанные события
                stats = statServiceImpl.getStats(startDate, endDate, uris, unique);
            }

            // Сортировка по убыванию hits
            stats.sort((a, b) -> Long.compare(b.getHits(), a.getHits()));

            log.info("Returning {} event stats records", stats.size());
            stats.forEach(s -> log.debug("  -> {}: {} hits", s.getUri(), s.getHits()));

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error processing /events stats: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
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
            // Полное URL декодирование
            start = URLDecoder.decode(start, StandardCharsets.UTF_8.toString());
            end = URLDecoder.decode(end, StandardCharsets.UTF_8.toString());

            log.info("Decoded params: start='{}', end='{}'", start, end);

            LocalDateTime startDate = LocalDateTime.parse(start, DateTimeFormatter.ofPattern(FORMATTER));
            LocalDateTime endDate = LocalDateTime.parse(end, DateTimeFormatter.ofPattern(FORMATTER));

            log.info("Parsed dates: start={}, end={}", startDate, endDate);

            if (startDate.isAfter(endDate)) {
                log.error("Invalid date range: start {} is after end {}", startDate, endDate);
                throw new IllegalArgumentException("Неверный диапазон дат: start не может быть после end");
            }

            // Создаем URI для конкретного события
            String eventUri = "/events/" + id;
            List<String> uris = List.of(eventUri);

            List<ViewStatsDto> stats = statServiceImpl.getStats(startDate, endDate, uris, unique);

            // Если статистики нет, возвращаем объект с 0 хитов
            if (stats.isEmpty()) {
                ViewStatsDto emptyStat = ViewStatsDto.builder()
                        .app("ewm-main")
                        .uri(eventUri)
                        .hits(0L)
                        .build();
                stats = List.of(emptyStat);
                log.info("No stats found for event {}, returning zero hits", id);
            } else {
                log.info("Found stats for event {}: {} hits", id, stats.get(0).getHits());
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error processing /events/{} stats: {}", id, e.getMessage(), e);

            // Возвращаем пустой объект с 0 хитов при ошибке
            String eventUri = "/events/" + id;
            ViewStatsDto errorStat = ViewStatsDto.builder()
                    .app("ewm-main")
                    .uri(eventUri)
                    .hits(0L)
                    .build();
            return ResponseEntity.ok(List.of(errorStat));
        }
    }
}