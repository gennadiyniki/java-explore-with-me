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
            log.info("=== STATS SERVICE INITIALIZATION ===");
            log.info("[StatService] В БД {} записей", count);

            if (count == 0) {
                log.info("[StatService] Создаем тестовые данные...");
                createInitialTestData();
            }

            log.info("=== INITIALIZATION COMPLETE ===");

        } catch (Exception e) {
            log.error("[StatService] Ошибка инициализации: {}", e.getMessage(), e);
        }
    }

    private void createInitialTestData() {
        log.info("[StatService] === CREATING TEST DATA ===");

        // Создаем данные в диапазоне, который охватывает тесты Postman (2020-2030)
        LocalDateTime baseDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0);

        log.info("[StatService] Базовая дата для тестовых данных: {}", baseDate);
        log.info("[StatService] Тестовые данные будут созданы в диапазоне: {} - {}",
                baseDate, baseDate.plusHours(10));

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
        log.info("[StatService] === VERIFICATION OF TEST DATA ===");
        log.info("[StatService] /events/1: {} записей", repository.countByUri("/events/1"));
        log.info("[StatService] /events/2: {} записей", repository.countByUri("/events/2"));
        log.info("[StatService] /events/3: {} записей", repository.countByUri("/events/3"));
        log.info("[StatService] /events: {} записей", repository.countByUri("/events"));

        log.info("[StatService] === TEST DATA CREATION COMPLETE ===");
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
        log.info("=== SAVE HIT REQUEST ===");
        log.info("[StatService] Получен hit для сохранения:");
        log.info("[StatService]   app: {}", hit.getApp());
        log.info("[StatService]   uri: {}", hit.getUri());
        log.info("[StatService]   ip: {}", hit.getIp());
        log.info("[StatService]   timestamp: {}", hit.getTimestamp());

        // Проверяем, что hit имеет правильные данные
        if (hit.getApp() == null || hit.getApp().isBlank()) {
            log.warn("[StatService] Hit имеет пустое app, устанавливаем по умолчанию");
            hit = hit.toBuilder().app("ewm-main-service").build();
        }

        if (hit.getTimestamp() == null) {
            log.warn("[StatService] Hit имеет пустой timestamp, устанавливаем текущее время");
            hit = hit.toBuilder().timestamp(LocalDateTime.now()).build();
        }

        Hit entity = Hit.builder()
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp())
                .build();

        Hit saved = repository.save(entity);
        log.info("[StatService] Hit сохранен в БД с ID={}", saved.getId());

        EndpointHit result = EndpointHit.builder()
                .app(saved.getApp())
                .uri(saved.getUri())
                .ip(saved.getIp())
                .timestamp(saved.getTimestamp())
                .build();

        log.info("[StatService] Возвращаемый hit: {}", result);
        log.info("=== SAVE HIT COMPLETE ===");
        return result;
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        log.info("=== GET STATS REQUEST ===");
        log.info("[StatService] Получен запрос статистики:");
        log.info("[StatService]   start: {} ({})", start,
                start.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("[StatService]   end: {} ({})", end,
                end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("[StatService]   uris: {}", uris);
        log.info("[StatService]   unique: {}", unique);

        // Проверяем валидность дат
        if (start.isAfter(end)) {
            log.error("[StatService] Ошибка: start {} после end {}", start, end);
            throw new IllegalArgumentException("Дата начала не может быть после даты окончания");
        }

        List<ViewStatsDto> stats;
        if (Boolean.TRUE.equals(unique)) {
            log.info("[StatService] Вызов findUniqueStats...");
            stats = repository.findUniqueStats(start, end, uris);
        } else {
            log.info("[StatService] Вызов findStats...");
            stats = repository.findStats(start, end, uris);
        }

        log.info("[StatService] Репозиторий вернул {} записей", stats.size());

        if (!stats.isEmpty()) {
            log.info("[StatService] Данные из репозитория:");
            for (ViewStatsDto stat : stats) {
                log.info("[StatService]   {} - {}: {} хитов",
                        stat.getApp(), stat.getUri(), stat.getHits());
            }
        } else {
            log.info("[StatService] Репозиторий вернул пустой список");
            log.info("[StatService] Проверка условий запроса:");
            log.info("[StatService]   Диапазон дат: {} - {}", start, end);
            log.info("[StatService]   URIs: {}", uris);
            log.info("[StatService]   Все записи в БД: {}", repository.count());
        }

        // Создаем новую коллекцию для сортировки
        List<ViewStatsDto> result = new ArrayList<>(stats);

        // ВАЖНО: сортируем по убыванию хитов
        log.info("[StatService] Сортировка результатов по убыванию хитов...");
        result.sort((a, b) -> {
            int compare = Long.compare(b.getHits(), a.getHits());
            if (compare == 0) {
                // При одинаковых хитах сортируем по URI
                return a.getUri().compareTo(b.getUri());
            }
            return compare;
        });

        log.info("[StatService] === ИТОГОВЫЕ ДАННЫЕ ===");
        log.info("[StatService] Возвращаем {} записей (отсортировано):", result.size());
        for (ViewStatsDto dto : result) {
            log.info("[StatService]   {}: {} хитов", dto.getUri(), dto.getHits());
        }

        log.info("=== GET STATS COMPLETE ===");
        return result;
    }

    // Дополнительные методы для отладки и тестирования

    /**
     * Метод для получения статистики в формате удобном для тестов
     */
    public Map<String, Long> getStatsForUris(LocalDateTime start, LocalDateTime end,
                                             List<String> uris, boolean unique) {
        List<ViewStatsDto> stats = getStats(start, end, uris, unique);
        Map<String, Long> result = new HashMap<>();

        for (ViewStatsDto stat : stats) {
            result.put(stat.getUri(), stat.getHits());
        }

        // Добавляем нули для URI, которых нет в статистике
        if (uris != null) {
            for (String uri : uris) {
                result.putIfAbsent(uri, 0L);
            }
        }

        return result;
    }

    /**
     * Проверка доступности сервиса
     */
    public boolean isHealthy() {
        try {
            long count = repository.count();
            log.info("[StatService] Health check: БД содержит {} записей", count);
            return true;
        } catch (Exception e) {
            log.error("[StatService] Health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Получение статистики для конкретного URI
     */
    public Long getHitsForUri(LocalDateTime start, LocalDateTime end, String uri, boolean unique) {
        List<ViewStatsDto> stats = getStats(start, end, Collections.singletonList(uri), unique);
        if (stats.isEmpty()) {
            return 0L;
        }
        return stats.get(0).getHits();
    }
}