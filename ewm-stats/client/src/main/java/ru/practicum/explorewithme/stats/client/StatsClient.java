package ru.practicum.explorewithme.stats.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.explorewithme.stats.dto.EndpointHit;
import ru.practicum.explorewithme.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {
    private final RestTemplate restTemplate;
    private static final String STATS_SERVER_URL = "http://localhost:9090";

    public void postHit(String app, String uri, String ip, LocalDateTime timestamp) {
        log.info("=== STATS CLIENT: POST HIT ===");
        log.info("App: {}, URI: {}, IP: {}, Timestamp: {}", app, uri, ip, timestamp);

        EndpointHit hit = EndpointHit.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();

        try {
            String url = STATS_SERVER_URL + "/hit";
            log.info("Sending POST to: {}", url);

            ResponseEntity<EndpointHit> response = restTemplate.postForEntity(url, hit, EndpointHit.class);
            log.info("Response status: {}", response.getStatusCode());
            log.info("Response body: {}", response.getBody());

        } catch (Exception e) {
            log.error("=== STATS CLIENT ERROR: {} ===", e.getMessage(), e);
        }
    }

    public ResponseEntity<List<ViewStats>> getStats(String start, String end, List<String> uris, Boolean unique) {
        log.info("=== STATS CLIENT: GET STATS ===");
        log.info("Params - start: '{}', end: '{}', uris: {}, unique: {}", start, end, uris, unique);

        try {
            // Проблема: тесты отправляют даты с пробелами, но не кодируют
            // Давайте проверим, что получает сервис статистики

            String url = STATS_SERVER_URL + "/stats?start=" + start + "&end=" + end;
            if (uris != null && !uris.isEmpty()) {
                url += "&uris=" + String.join(",", uris);
            }
            if (unique != null) {
                url += "&unique=" + unique;
            }

            log.info("Request URL: {}", url);

            ResponseEntity<List<ViewStats>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStats>>() {}
            );

            log.info("Response status: {}", response.getStatusCode());
            log.info("Response body: {}", response.getBody());

            return response;

        } catch (Exception e) {
            log.error("=== STATS CLIENT ERROR: {} ===", e.getMessage(), e);
            return ResponseEntity.ok(List.of()); // Возвращаем пустой список при ошибке
        }
    }
}