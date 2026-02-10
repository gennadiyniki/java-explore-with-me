package ru.practicum.explorewithme.stats.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class StatsController {
    private final StatServiceImpl statServiceImpl;

    @PostMapping("/hit")
    public ResponseEntity<EndpointHit> hit(@Valid @RequestBody EndpointHit endpointHit) {
        log.info("POST /hit: {}", endpointHit);
        EndpointHit savedHit = statServiceImpl.saveHit(endpointHit);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedHit);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStats>> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {

        log.info("GET /stats: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startDate = LocalDateTime.parse(start, formatter);
            LocalDateTime endDate = LocalDateTime.parse(end, formatter);

            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().build();
            }

            List<ViewStats> stats = statServiceImpl.getStats(startDate, endDate, uris, unique);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}