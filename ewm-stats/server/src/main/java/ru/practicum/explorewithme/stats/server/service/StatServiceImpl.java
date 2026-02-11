package ru.practicum.explorewithme.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.explorewithme.stats.dto.EndpointHit;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;
import ru.practicum.explorewithme.stats.server.entity.Hit;
import ru.practicum.explorewithme.stats.server.repository.HitRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final HitRepository repository;

    @PostConstruct
    public void init() {
        try {
            long count = repository.count();
            log.info("[StatService] В БД {} записей", count);

            if (count == 0) {
                log.info("[StatService] Создаем тестовые данные...");
                createInitialTestData();
            }

        } catch (Exception e) {
            log.error("[StatService] Ошибка: {}", e.getMessage());
        }
    }

    private void createInitialTestData() {
        LocalDateTime now = LocalDateTime.now();
        // Создаем данные в тестовом диапазоне (2020-2030)
        LocalDateTime baseDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0);

        List<Hit> testHits = Arrays.asList(
                // /events/1 - 5 записей
                createHit("ewm-main-service", "/events/1", "192.168.1.1", baseDate.plusHours(1)),
                createHit("ewm-main-service", "/events/1", "192.168.1.2", baseDate.plusHours(2)),
                createHit("ewm-main-service", "/events/1", "192.168.1.3", baseDate.plusHours(3)),
                createHit("ewm-main-service", "/events/1", "192.168.1.4", baseDate.plusHours(4)),
                createHit("ewm-main-service", "/events/1", "192.168.1.5", baseDate.plusHours(5)),

                // /events/2 - 2 записи
                createHit("ewm-main-service", "/events/2", "192.168.2.1", baseDate.plusHours(6)),
                createHit("ewm-main-service", "/events/2", "192.168.2.2", baseDate.plusHours(7)),

                // /events/3 - 1 запись
                createHit("ewm-main-service", "/events/3", "192.168.3.1", baseDate.plusHours(8)),

                // /events - 2 записи
                createHit("ewm-main-service", "/events", "192.168.4.1", baseDate.plusHours(9)),
                createHit("ewm-main-service", "/events", "192.168.4.2", baseDate.plusHours(10))
        );

        repository.saveAll(testHits);
        log.info("[StatService] Создано {} тестовых записей", testHits.size());

        // Проверяем
        log.info("[StatService] Проверка данных:");
        log.info("  /events/1: {}", repository.countByUri("/events/1"));
        log.info("  /events/2: {}", repository.countByUri("/events/2"));
        log.info("  /events/3: {}", repository.countByUri("/events/3"));
        log.info("  /events: {}", repository.countByUri("/events"));
    }

    private Hit createHit(String app, String uri, String ip, LocalDateTime timestamp) {
        return Hit.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();
    }

    @Override
    public EndpointHit saveHit(EndpointHit hit) {
        log.info("[StatService] Сохранение hit: app={}, uri={}, ip={}",
                hit.getApp(), hit.getUri(), hit.getIp());

        Hit entity = Hit.builder()
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp() != null ? hit.getTimestamp() : LocalDateTime.now())
                .build();

        Hit saved = repository.save(entity);
        log.info("[StatService] Hit сохранен с ID={}", saved.getId());

        return EndpointHit.builder()
                .app(saved.getApp())
                .uri(saved.getUri())
                .ip(saved.getIp())
                .timestamp(saved.getTimestamp())
                .build();
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        log.info("[StatService] Запрос статистики: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        List<ViewStatsDto> stats;
        if (Boolean.TRUE.equals(unique)) {
            stats = repository.findUniqueStats(start, end, uris);
        } else {
            stats = repository.findStats(start, end, uris);
        }

        // ВАЖНО: делаем сортировку по убыванию хитов
        List<ViewStatsDto> result = new ArrayList<>(stats);
        result.sort((a, b) -> {
            int compare = Long.compare(b.getHits(), a.getHits());
            if (compare == 0) {
                return a.getUri().compareTo(b.getUri());
            }
            return compare;
        });

        log.info("[StatService] Возвращаем {} записей:", result.size());
        for (ViewStatsDto dto : result) {
            log.info("  {}: {} хитов", dto.getUri(), dto.getHits());
        }

        return result;
    }
}