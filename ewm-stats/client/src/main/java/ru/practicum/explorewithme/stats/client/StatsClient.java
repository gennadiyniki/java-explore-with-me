package ru.practicum.explorewithme.stats.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.explorewithme.stats.dto.EndpointHit;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StatsClient {
    private final RestTemplate restTemplate;
    private static final String STATS_SERVER_URL = "http://localhost:9090";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient() {
        this.restTemplate = createRestTemplate();
        log.info("[StatsClient] Инициализирован");
    }

    private RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Настраиваем ObjectMapper для правильной сериализации LocalDateTime
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // Создаем конвертер с настроенным ObjectMapper
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);

        // Добавляем конвертер в RestTemplate
        restTemplate.getMessageConverters().add(0, converter);

        return restTemplate;
    }

    public void saveHit(EndpointHit hit) {
        log.info("[StatsClient] Отправка hit: app={}, uri={}, ip={}",
                hit.getApp(), hit.getUri(), hit.getIp());

        try {
            String url = STATS_SERVER_URL + "/hit";

            ResponseEntity<EndpointHit> response = restTemplate.postForEntity(
                    url,
                    hit,
                    EndpointHit.class
            );

            log.info("[StatsClient] Hit успешно отправлен, статус: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("[StatsClient] Ошибка отправки hit: {}", e.getMessage());
            // Не бросаем исключение, чтобы не ломать основной функционал
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        log.info("[StatsClient] Запрос статистики: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        try {
            // Форматируем даты
            String startStr = start.format(FORMATTER);
            String endStr = end.format(FORMATTER);

            // Кодируем для URL (пробелы -> %20)
            String encodedStart = URLEncoder.encode(startStr, StandardCharsets.UTF_8.toString());
            String encodedEnd = URLEncoder.encode(endStr, StandardCharsets.UTF_8.toString());

            // Строим URL
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(STATS_SERVER_URL + "/stats")
                    .queryParam("start", encodedStart)
                    .queryParam("end", encodedEnd)
                    .queryParam("unique", unique);

            // Добавляем URIs как отдельные параметры
            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    builder.queryParam("uris", uri);
                }
            }

            String url = builder.toUriString();
            log.debug("[StatsClient] URL запроса: {}", url);

            // Выполняем запрос
            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );

            List<ViewStatsDto> stats = response.getBody();
            log.info("[StatsClient] Получено {} записей статистики",
                    stats != null ? stats.size() : 0);

            return stats != null ? stats : Collections.emptyList();

        } catch (Exception e) {
            log.error("[StatsClient] Ошибка получения статистики: {}", e.getMessage());
            return Collections.emptyList();
        }
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
    public List<ViewStatsDto> getStatsForUri(LocalDateTime start, LocalDateTime end, String uri, boolean unique) {
        return getStats(start, end, Collections.singletonList(uri), unique);
    }

    // Метод для получения хитов по одному URI
    public Long getHitsForUri(LocalDateTime start, LocalDateTime end, String uri, boolean unique) {
        List<ViewStatsDto> stats = getStatsForUri(start, end, uri, unique);
        if (stats.isEmpty()) {
            return 0L;
        }
        return stats.get(0).getHits();
    }
}