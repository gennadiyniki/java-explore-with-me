package ru.practicum.explorewithme.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.explorewithme.stats.dto.EndpointHit;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;
import ru.practicum.explorewithme.stats.server.entity.Hit;
import ru.practicum.explorewithme.stats.server.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final HitRepository repository;

    @Override
    public EndpointHit saveHit(EndpointHit hit) {
        log.info("[StatService] Сохранение статистики: app={}, uri={}, ip={}, timestamp={}",
                hit.getApp(), hit.getUri(), hit.getIp(), hit.getTimestamp());

        Hit entity = Hit.fromDto(hit);
        log.debug("[StatService] Преобразованная entity: {}", entity);

        Hit saved = repository.save(entity);

        log.info("[StatService] Статистика сохранена с ID={}: app={}, uri={}",
                saved.getId(), hit.getApp(), hit.getUri());
        return hit;
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("[StatService] Получение статистики: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        // Проверяем, есть ли данные в БД
        long totalHits = repository.count();
        log.info("[StatService] Всего записей в БД: {}", totalHits);

        // Выводим несколько записей для отладки
        List<Hit> allHits = repository.findAll();
        if (!allHits.isEmpty()) {
            log.info("[StatService] Пример записи из БД: app={}, uri={}, timestamp={}",
                    allHits.get(0).getApp(), allHits.get(0).getUri(), allHits.get(0).getTimestamp());
        }

        List<ViewStatsDto> stats;
        if (unique) {
            stats = repository.findUniqueStats(start, end, uris);
            log.info("[StatService] Получена уникальная статистика: {} записей", stats.size());
        } else {
            stats = repository.findStats(start, end, uris);
            log.info("[StatService] Получена полная статистика: {} записей", stats.size());
        }

        // Логируем результат
        for (ViewStatsDto stat : stats) {
            log.info("[StatService] Stat: app={}, uri={}, hits={}",
                    stat.getApp(), stat.getUri(), stat.getHits());
        }

        return stats;
    }
}