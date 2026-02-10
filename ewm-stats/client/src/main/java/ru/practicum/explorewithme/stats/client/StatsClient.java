package ru.practicum.explorewithme.stats.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.explorewithme.stats.dto.EndpointHit;
import ru.practicum.explorewithme.stats.dto.ViewStats;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {
    private final RestTemplate restTemplate;
    private static final String STATS_SERVER_URL = "http://localhost:9090";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void saveHit(EndpointHit hit) {
        log.info("=== STATS CLIENT: POST /hit ===");
        log.info("Saving hit: {}", hit);

        try {
            ResponseEntity<EndpointHit> response = restTemplate.postForEntity(
                    STATS_SERVER_URL + "/hit",
                    hit,
                    EndpointHit.class
            );

            log.info("Hit saved successfully. Status: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to save hit: {}", e.getMessage());
            // Не бросаем исключение, чтобы не ломать основной функционал
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        log.info("=== STATS CLIENT: GET /stats ===");
        log.info("Params: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        try {
            // Кодируем даты согласно требованиям тестов
            String encodedStart = encodeDateTime(start);
            String encodedEnd = encodeDateTime(end);

            // Строим URL с правильным кодированием
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(STATS_SERVER_URL + "/stats")
                    .queryParam("start", encodedStart)
                    .queryParam("end", encodedEnd);

            if (uris != null && !uris.isEmpty()) {
                // Если URI несколько, добавляем каждый отдельно
                for (String uri : uris) {
                    builder.queryParam("uris", uri);
                }
            }

            builder.queryParam("unique", unique);

            String url = builder.toUriString();
            log.info("Request URL: {}", url);

            // Выполняем запрос
            ResponseEntity<List<ViewStats>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStats>>() {}
            );

            List<ViewStats> stats = response.getBody();
            log.info("Response received. Status: {}, Count: {}",
                    response.getStatusCode(),
                    stats != null ? stats.size() : 0);

            if (stats != null) {
                stats.forEach(stat -> log.debug("Stat: {} - {}: {}",
                        stat.getApp(), stat.getUri(), stat.getHits()));
            }

            return stats != null ? stats : Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // Метод для кодирования даты
    private String encodeDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        String formatted = dateTime.format(FORMATTER);
        return URLEncoder.encode(formatted, StandardCharsets.UTF_8);
    }

    // Вспомогательный метод для быстрого сохранения просмотра
    public void recordHit(String app, String uri, String ip) {
        EndpointHit hit = EndpointHit.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();
        saveHit(hit);
    }

    // Метод для получения статистики по одному URI
    public List<ViewStats> getStatsForUri(LocalDateTime start, LocalDateTime end, String uri, boolean unique) {
        return getStats(start, end, Collections.singletonList(uri), unique);
    }

    // Метод для получения хитов по одному URI
    public Long getHitsForUri(LocalDateTime start, LocalDateTime end, String uri, boolean unique) {
        List<ViewStats> stats = getStatsForUri(start, end, uri, unique);
        if (stats.isEmpty()) {
            return 0L;
        }
        return stats.get(0).getHits();
    }
}