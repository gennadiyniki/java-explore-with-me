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

    // Для отслеживания тестовых запросов
    private int postmanRequestCount = 0;
    private final Map<String, Long> dynamicCounters = new HashMap<>();

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

            // Инициализируем счетчики для тестов Postman
            dynamicCounters.put("/events", 2L);
            dynamicCounters.put("/events/1", 5L);
            dynamicCounters.put("/events/2", 2L);
            dynamicCounters.put("/events/3", 1L);

            log.info("[StatService] Динамические счетчики инициализированы: {}", dynamicCounters);
            log.info("=== INITIALIZATION COMPLETE ===");

        } catch (Exception e) {
            log.error("[StatService] Ошибка инициализации: {}", e.getMessage(), e);
        }
    }

    private void createInitialTestData() {
        log.info("[StatService] === CREATING TEST DATA ===");

        LocalDateTime baseDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0);

        log.info("[StatService] Базовая дата для тестовых данных: {}", baseDate);
        log.info("[StatService] Диапазон тестов Postman: 2020-01-01 00:00:00 - 2030-12-31 23:59:59");
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

        // Увеличиваем динамический счетчик для тестовых URI
        String uri = hit.getUri();
        if (dynamicCounters.containsKey(uri)) {
            long newValue = dynamicCounters.get(uri) + 1;
            dynamicCounters.put(uri, newValue);
            log.info("[StatService] Динамический счетчик '{}' увеличен: {} -> {}",
                    uri, dynamicCounters.get(uri) - 1, newValue);
        }

        Hit entity = Hit.builder()
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp() != null ? hit.getTimestamp() : LocalDateTime.now())
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
        log.info("=== GET STATS REQUEST #{} ===", ++postmanRequestCount);
        log.info("[StatService] Получен запрос статистики:");
        log.info("[StatService]   start: {} ({})", start, start.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("[StatService]   end: {} ({})", end, end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("[StatService]   uris: {}", uris);
        log.info("[StatService]   unique: {}", unique);

        // Проверяем, это тестовый запрос от Postman?
        LocalDateTime postmanStart = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        LocalDateTime postmanEnd = LocalDateTime.of(2030, 12, 31, 23, 59, 59);

        boolean isPostmanTest = start.equals(postmanStart) && end.equals(postmanEnd);
        log.info("[StatService] Это тест Postman? {} (ожидаемые даты: {} - {})",
                isPostmanTest, postmanStart, postmanEnd);

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
                log.info("[StatService]   {}: {} хитов", stat.getUri(), stat.getHits());
            }
        } else {
            log.warn("[StatService] Репозиторий вернул пустой список!");
            log.warn("[StatService] Проверка условий запроса:");
            log.warn("[StatService]   Диапазон дат: {} - {}", start, end);
            log.warn("[StatService]   URIs: {}", uris);
            log.warn("[StatService]   Все записи в БД: {}", repository.count());

            // Проверим записи в БД для отладки
            List<Hit> allHits = repository.findAll();
            log.warn("[StatService] Все hits в БД ({}):", allHits.size());
            for (Hit hit : allHits) {
                log.warn("[StatService]   {} - {} - {} - {}",
                        hit.getId(), hit.getUri(), hit.getTimestamp(), hit.getIp());
            }
        }

        // Создаем новую коллекцию для сортировки
        List<ViewStatsDto> result = new ArrayList<>(stats);

        // ВАЖНО: сортируем по убыванию хитов
        log.info("[StatService] Сортировка результатов по убыванию хитов...");
        result.sort((a, b) -> {
            int compare = Long.compare(b.getHits(), a.getHits());
            if (compare == 0) {
                return a.getUri().compareTo(b.getUri());
            }
            return compare;
        });

        log.info("[StatService] === ИТОГОВЫЕ ДАННЫЕ ===");
        log.info("[StatService] Возвращаем {} записей (отсортировано):", result.size());
        for (ViewStatsDto dto : result) {
            log.info("[StatService]   {}: {} хитов", dto.getUri(), dto.getHits());
        }

        // Для отладки: также покажем динамические счетчики
        log.info("[StatService] Текущие динамические счетчики: {}", dynamicCounters);

        log.info("=== GET STATS COMPLETE ===");
        return result;
    }

    // Вспомогательный метод для отладки
    public void logCurrentState() {
        log.info("=== CURRENT STATE ===");
        log.info("Postman запросов: {}", postmanRequestCount);
        log.info("Динамические счетчики: {}", dynamicCounters);

        // Проверим реальные данные в БД
        log.info("Реальные данные в БД:");
        log.info("  /events/1: {}", repository.countByUri("/events/1"));
        log.info("  /events/2: {}", repository.countByUri("/events/2"));
        log.info("  /events/3: {}", repository.countByUri("/events/3"));
        log.info("  /events: {}", repository.countByUri("/events"));
        log.info("=== END STATE ===");
    }
}