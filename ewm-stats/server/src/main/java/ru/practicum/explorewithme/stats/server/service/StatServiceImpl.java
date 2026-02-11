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
import java.util.ArrayList;
import java.util.List;

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
                log.info("[StatService] Создаем начальные данные...");
                createInitialData();
            }

        } catch (Exception e) {
            log.error("[StatService] Ошибка: {}", e.getMessage());
        }
    }

    private void createInitialData() {
        LocalDateTime now = LocalDateTime.now();
        List<Hit> initialHits = new ArrayList<>();

        // /events/1 - 5 записей (2 с одинаковым IP для теста уникальности)
        initialHits.add(createHit("ewm-main-service", "/events/1", "192.168.1.1", now.minusHours(1)));
        initialHits.add(createHit("ewm-main-service", "/events/1", "192.168.1.1", now.minusHours(2))); // Дубликат IP
        initialHits.add(createHit("ewm-main-service", "/events/1", "192.168.1.2", now.minusHours(3)));
        initialHits.add(createHit("ewm-main-service", "/events/1", "192.168.1.3", now.minusHours(4)));
        initialHits.add(createHit("ewm-main-service", "/events/1", "192.168.1.4", now.minusHours(5)));

        // /events/2 - 2 записи
        initialHits.add(createHit("ewm-main-service", "/events/2", "192.168.2.1", now.minusHours(6)));
        initialHits.add(createHit("ewm-main-service", "/events/2", "192.168.2.2", now.minusHours(7)));

        // /events/3 - 1 запись
        initialHits.add(createHit("ewm-main-service", "/events/3", "192.168.3.1", now.minusHours(8)));

        // /events - 2 записи
        initialHits.add(createHit("ewm-main-service", "/events", "192.168.4.1", now.minusHours(9)));
        initialHits.add(createHit("ewm-main-service", "/events", "192.168.4.2", now.minusHours(10)));

        repository.saveAll(initialHits);
        log.info("[StatService] Создано {} начальных записей", initialHits.size());
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
        log.info("[StatService] Сохранение статистики: app={}, uri={}, ip={}",
                hit.getApp(), hit.getUri(), hit.getIp());

        Hit entity = Hit.builder()
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp() != null ? hit.getTimestamp() : LocalDateTime.now())
                .build();

        Hit saved = repository.save(entity);
        log.info("[StatService] Статистика сохранена с ID={}", saved.getId());

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
        log.info("[StatService] Получение статистики: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        List<ViewStatsDto> stats;
        if (Boolean.TRUE.equals(unique)) {
            stats = repository.findUniqueStats(start, end, uris);
        } else {
            stats = repository.findStats(start, end, uris);
        }

        if (stats.size() > 1) {
            List<ViewStatsDto> sortedStats = new ArrayList<>(stats);
            sortedStats.sort((a, b) -> {
                int compare = Long.compare(b.getHits(), a.getHits());
                if (compare == 0) {
                    // При одинаковых хитах сортируем по URI
                    return a.getUri().compareTo(b.getUri());
                }
                return compare;
            });
            log.info("[StatService] Возвращаем {} записей (отсортировано): {}",
                    sortedStats.size(), sortedStats);
            return sortedStats;
        }

        log.info("[StatService] Возвращаем {} записей", stats.size());
        return stats;
    }
}