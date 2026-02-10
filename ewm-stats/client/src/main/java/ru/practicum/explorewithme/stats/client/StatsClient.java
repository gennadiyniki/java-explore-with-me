package ru.practicum.explorewithme.stats.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.explorewithme.stats.dto.EndpointHit;
import ru.practicum.explorewithme.stats.dto.ViewStats;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatsClient {
    private final RestTemplate restTemplate;
    private static final String STATS_SERVER_URL = "http://localhost:9090";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void postHit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHit hit = EndpointHit.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();
        try {
            String url = STATS_SERVER_URL + "/hit";
            restTemplate.postForObject(url, hit, EndpointHit.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Ошибка при сохранении статистики: " + e.getStatusCode());
        }
    }

    public ResponseEntity<List<ViewStats>> getStats(String start, String end, List<String> uris, Boolean unique) {
        try {
            // Кодируем параметры
            String encodedStart = URLEncoder.encode(start, StandardCharsets.UTF_8.toString());
            String encodedEnd = URLEncoder.encode(end, StandardCharsets.UTF_8.toString());

            // Строим URL с правильным кодированием
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(STATS_SERVER_URL + "/stats")
                    .queryParam("start", encodedStart)
                    .queryParam("end", encodedEnd);

            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", String.join(",", uris));
            }

            if (unique != null) {
                builder.queryParam("unique", unique);
            }

            String url = builder.toUriString();

            return restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStats>>() {
                    },
                    (Object) null
            );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении статистики: " + e.getMessage(), e);
        }
    }

    public static String clientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        }
        return xForwardedForHeader.split(",")[0].trim();
    }
}