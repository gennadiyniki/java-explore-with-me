package ru.practicum.ewm.stats.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {

    private final RestTemplate restTemplate;

    @Value("${stats.server.url:http://localhost:9090}")
    private String serverUrl;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void saveHit(EndpointHitDto hit) {
        if (hit == null) {
            log.warn("Attempt to save null hit");
            return;
        }

        log.debug("Sending hit to stats server: app={}, uri={}",
                hit.getApp(), hit.getUri());

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    serverUrl + "/hit",
                    hit,
                    Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Hit saved successfully to {}", serverUrl);
            } else {
                log.warn("Stats server returned status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to send hit to {}: {}", serverUrl, e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       boolean unique,
                                       List<String> uris) {

        if (start == null || end == null) {
            log.warn("Start or end date is null");
            return List.of();
        }

        if (end.isBefore(start)) {
            log.warn("End date {} is before start date {}", end, start);
            return List.of();
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(serverUrl + "/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                uriBuilder.queryParam("uris", uri);  // Добавляем каждый URI отдельно
            }
        }

        String url = uriBuilder.encode().toUriString();
        log.debug("Requesting stats from: {}", url);

        try {
            ResponseEntity<ViewStatsDto[]> response = restTemplate.getForEntity(
                    url,
                    ViewStatsDto[].class
            );

            ViewStatsDto[] body = response.getBody();
            int count = body != null ? body.length : 0;
            log.debug("Received {} stats records from {}", count, serverUrl);

            return body != null ? List.of(body) : List.of();

        } catch (Exception e) {
            log.error("Failed to fetch stats from {}: {}", serverUrl, e.getMessage());
            return List.of();
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       boolean unique) {
        return getStats(start, end, unique, null);
    }
}