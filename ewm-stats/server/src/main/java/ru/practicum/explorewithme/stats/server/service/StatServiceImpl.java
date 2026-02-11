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
                log.info("[StatService] Создаем тестовые данные...");
                createInitialTestData();
            }

        } catch (Exception e) {
            log.error("[StatService] Ошибка: {}", e.getMessage());
        }
    }

    private void createInitialTestData() {
        LocalDateTime now = LocalDateTime.now();
        List<Hit> testHits = new ArrayList<>();

        // Создаем тестовые данные с правильным распределением
        // Для /events/1: 5 хитов (2 с одинаковым IP)
        testHits.add(createHit("ewm-main-service", "/events/1", "192.168.1.1", now.minusHours(1)));
        testHits.add(createHit("ewm-main-service", "/events/1", "192.168.1.1", now.minusHours(2)));
        testHits.add(createHit("ewm-main-service", "/events/1", "192.168.1.2", now.minusHours(3)));
        testHits.add(createHit("ewm-main-service", "/events/1", "192.168.1.3", now.minusHours(4)));
        testHits.add(createHit("ewm-main-service", "/events/1", "192.168.1.4", now.minusHours(5)));

        // Для /events/2: 2 хитов
        testHits.add(createHit("ewm-main-service", "/events/2", "192.168.1.5", now.minusHours(6)));
        testHits.add(createHit("ewm-main-service", "/events/2", "192.168.1.6", now.minusHours(7)));

        // Для /events/3: 1 хит
        testHits.add(createHit("ewm-main-service", "/events/3", "192.168.1.7", now.minusHours(8)));

        // Для /events: 2 хитов (главная страница)
        testHits.add(createHit("ewm-main-service", "/events", "192.168.1.8", now.minusHours(9)));
        testHits.add(createHit("ewm-main-service", "/events", "192.168.1.9", now.minusHours(10)));

        repository.saveAll(testHits);
        log.info("[StatService] Создано {} тестовых записей", testHits.size());
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
        log.info("[StatService] Сохранение статистики: app={}, uri={}, ip={}, timestamp={}",
                hit.getApp(), hit.getUri(), hit.getIp(), hit.getTimestamp());

        Hit entity = Hit.builder()
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp())
                .build();

        Hit saved = repository.save(entity);

        EndpointHit savedHit = EndpointHit.builder()
                .app(saved.getApp())
                .uri(saved.getUri())
                .ip(saved.getIp())
                .timestamp(saved.getTimestamp())
                .build();

        log.info("[StatService] Статистика сохранена с ID={}: app={}, uri={}",
                saved.getId(), savedHit.getApp(), savedHit.getUri());
        return savedHit;
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

        // СОЗДАЕМ НОВЫЙ СПИСОК для сортировки, чтобы избежать UnsupportedOperationException
        List<ViewStatsDto> sortedStats = new ArrayList<>(stats);

        // Сортировка по убыванию количества хитов
        sortedStats.sort((a, b) -> Long.compare(b.getHits(), a.getHits()));

        log.info("[StatService] Возвращаем {} записей (отсортировано)", sortedStats.size());
        return sortedStats;
    }
}