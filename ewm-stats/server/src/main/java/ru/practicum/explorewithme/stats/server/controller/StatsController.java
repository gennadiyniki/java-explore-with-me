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
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class StatsController {
    private final StatService statService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/hit")
    public ResponseEntity<EndpointHit> hit(@Valid @RequestBody EndpointHit endpointHit) {
        log.info("=== POST /hit ===");
        log.info("Принят hit: app={}, uri={}, ip={}, timestamp={}",
                endpointHit.getApp(), endpointHit.getUri(),
                endpointHit.getIp(), endpointHit.getTimestamp());

        EndpointHit savedHit = statService.saveHit(endpointHit);

        log.info("Hit сохранен с ID: {}", savedHit);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedHit);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStatsDto>> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {

        log.info("=== GET /stats ===");
        log.info("Получен запрос статистики:");
        log.info("  start (raw): '{}'", start);
        log.info("  end (raw): '{}'", end);
        log.info("  uris: {}", uris);
        log.info("  unique: {}", unique);

        try {
            // Декодирование URL (тесты Postman кодируют пробелы как %20)
            String decodedStart = URLDecoder.decode(start, StandardCharsets.UTF_8.toString());
            String decodedEnd = URLDecoder.decode(end, StandardCharsets.UTF_8.toString());

            log.info("  start (decoded): '{}'", decodedStart);
            log.info("  end (decoded): '{}'", decodedEnd);

            LocalDateTime startDate = LocalDateTime.parse(decodedStart, FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(decodedEnd, FORMATTER);

            log.info("  start (parsed): {}", startDate);
            log.info("  end (parsed): {}", endDate);

            if (startDate.isAfter(endDate)) {
                log.error("Ошибка: дата начала {} позже даты окончания {}", startDate, endDate);
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            List<ViewStatsDto> stats = statService.getStats(startDate, endDate, uris, unique);

            log.info("Возвращаем {} записей статистики:", stats.size());
            for (ViewStatsDto stat : stats) {
                log.info("  {}: {} - {} хитов", stat.getApp(), stat.getUri(), stat.getHits());
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Ошибка обработки запроса статистики: {}", e.getMessage(), e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}