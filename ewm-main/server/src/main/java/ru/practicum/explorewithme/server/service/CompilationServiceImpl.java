package ru.practicum.explorewithme.server.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.compilation.dto.CompilationDto;
import ru.practicum.explorewithme.compilation.dto.NewCompilationDto;
import ru.practicum.explorewithme.compilation.dto.UpdateCompilationRequest;
import ru.practicum.explorewithme.event.dto.EventShortDto;
import ru.practicum.explorewithme.server.repository.CompilationRepository;
import ru.practicum.explorewithme.server.repository.RequestRepository;
import ru.practicum.explorewithme.server.entity.Compilation;
import ru.practicum.explorewithme.server.entity.Event;
import ru.practicum.explorewithme.server.exception.EntityNotFoundException;
import ru.practicum.explorewithme.server.mapper.CompilationMapper;
import ru.practicum.explorewithme.server.mapper.EventMapper;
import ru.practicum.explorewithme.stats.client.StatsClient;
import ru.practicum.explorewithme.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final EventService eventService;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String COMPILATION_NOT_FOUND = "Подборка с id=%d не найдена";
    private static final String COMPILATION_TITLE_EXISTS = "Название подборки уже существует: %s";
    private static final boolean DEFAULT_PINNED = false;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto newCompilation) {
        log.info("[CompilationService] Создание подборки: {}", newCompilation.getTitle());

        if (compilationRepository.existsByTitle(newCompilation.getTitle())) {
            log.warn("[CompilationService] Подборка с названием '{}' уже существует", newCompilation.getTitle());
            throw new IllegalStateException(String.format(COMPILATION_TITLE_EXISTS, newCompilation.getTitle()));
        }

        Compilation compilation = Compilation.builder()
                .title(newCompilation.getTitle())
                .pinned(newCompilation.getPinned() != null ? newCompilation.getPinned() : DEFAULT_PINNED)
                .build();

        Set<Event> events = new HashSet<>();
        if (newCompilation.getEvents() != null && !newCompilation.getEvents().isEmpty()) {
            newCompilation.getEvents().forEach(eventId -> {
                try {
                    events.add(eventService.getById(eventId));
                } catch (Exception e) {
                    log.error("[CompilationService] Ошибка при получении события {}: {}", eventId, e.getMessage());
                    throw e;
                }
            });
        }
        compilation.setEvents(events);

        compilation = compilationRepository.save(compilation);
        entityManager.flush();

        log.info("[CompilationService] Подборка создана: id={}, title={}, events={}",
                compilation.getId(), compilation.getTitle(), events.size());

        // Batch получение статистики для всех событий
        List<EventShortDto> eventDtos = getEventShortDtosBatch(new ArrayList<>(events));

        return compilationMapper.toDto(compilation, eventDtos);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest update) {
        log.info("[CompilationService] Обновление подборки: id={}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> {
                    log.error("[CompilationService] Подборка не найдена: id={}", compId);
                    return new EntityNotFoundException(String.format(COMPILATION_NOT_FOUND, compId));
                });

        if (update.getTitle() != null) {
            if (compilationRepository.existsByTitleAndIdNot(update.getTitle(), compId)) {
                log.warn("[CompilationService] Название подборки уже занято: {}", update.getTitle());
                throw new IllegalStateException(String.format(COMPILATION_TITLE_EXISTS, update.getTitle()));
            }
            compilation.setTitle(update.getTitle());
        }

        if (update.getPinned() != null) {
            compilation.setPinned(update.getPinned());
        }

        Set<Event> events = new HashSet<>();
        if (update.getEvents() != null && !update.getEvents().isEmpty()) {
            update.getEvents().forEach(eventId -> {
                try {
                    events.add(eventService.getById(eventId));
                } catch (Exception e) {
                    log.error("[CompilationService] Ошибка при получении события {}: {}", eventId, e.getMessage());
                    throw e;
                }
            });
        }
        compilation.setEvents(events);

        compilation = compilationRepository.save(compilation);

        log.info("[CompilationService] Подборка обновлена: id={}, events={}", compId, events.size());

        // Batch получение статистики для всех событий
        List<EventShortDto> eventDtos = getEventShortDtosBatch(new ArrayList<>(events));

        return compilationMapper.toDto(compilation, eventDtos);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        log.info("[CompilationService] Удаление подборки: id={}", compId);

        if (!compilationRepository.existsById(compId)) {
            log.error("[CompilationService] Подборка не найдена для удаления: id={}", compId);
            throw new EntityNotFoundException(String.format(COMPILATION_NOT_FOUND, compId));
        }

        compilationRepository.deleteById(compId);
        log.info("[CompilationService] Подборка удалена: id={}", compId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size) {
        log.debug("[CompilationService] Получение подборок: pinned={}, from={}, size={}", pinned, from, size);

        PageRequest pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations;

        if (pinned != null) {
            Page<Compilation> page = compilationRepository.findAllByPinned(pinned, pageable);
            compilations = page.getContent();
        } else {
            Page<Compilation> page = compilationRepository.findAll(pageable);
            compilations = page.getContent();
        }

        log.debug("[CompilationService] Найдено {} подборок", compilations.size());

        // Собираем ВСЕ события из ВСЕХ подборок
        List<Event> allEvents = compilations.stream()
                .filter(comp -> comp.getEvents() != null)
                .flatMap(comp -> comp.getEvents().stream())
                .distinct()
                .collect(Collectors.toList());

        // Batch получение статистики для всех событий
        Map<Long, EventShortDto> eventDtoMap = getEventShortDtosBatch(allEvents).stream()
                .collect(Collectors.toMap(
                        EventShortDto::getId,
                        dto -> dto,
                        (d1, d2) -> d1
                ));

        // Собираем результат
        return compilations.stream()
                .map(comp -> {
                    List<EventShortDto> eventDtos = comp.getEvents() != null ?
                            comp.getEvents().stream()
                                    .map(event -> eventDtoMap.get(event.getId()))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList()) : List.of();
                    return compilationMapper.toDto(comp, eventDtos);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getById(Long compId) {
        log.debug("[CompilationService] Получение подборки: id={}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> {
                    log.error("[CompilationService] Подборка не найдена: id={}", compId);
                    return new EntityNotFoundException(String.format(COMPILATION_NOT_FOUND, compId));
                });

        if (compilation.getEvents() == null || compilation.getEvents().isEmpty()) {
            return compilationMapper.toDto(compilation, List.of());
        }

        // Batch получение статистики для всех событий подборки
        List<EventShortDto> eventDtos = getEventShortDtosBatch(new ArrayList<>(compilation.getEvents()));

        log.debug("[CompilationService] Подборка найдена: id={}, events={}", compId, eventDtos.size());
        return compilationMapper.toDto(compilation, eventDtos);
    }

    private List<EventShortDto> getEventShortDtosBatch(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        // Собираем ID всех событий
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        // 1 запрос для получения подтвержденных заявок для всех событий
        Map<Long, Long> confirmedCounts = requestRepository.getConfirmedCountsMap(eventIds);

        // 1 запрос для получения просмотров для всех событий
        Map<Long, Long> eventViews = getViewsBatch(eventIds);

        // Создаем DTO для всех событий
        return events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        confirmedCounts.getOrDefault(event.getId(), 0L),
                        eventViews.getOrDefault(event.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getViewsBatch(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // Создаем URIs для всех событий
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            // Устанавливаем широкий диапазон дат
            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now().plusYears(100);

            // Получаем статистику - unique=false для всех просмотров
            List<ViewStats> stats = statsClient.getStats(start, end, uris, false);

            log.debug("[CompilationService] Получено {} записей статистики для {} событий",
                    stats != null ? stats.size() : 0, eventIds.size());

            if (stats != null && !stats.isEmpty()) {
                return stats.stream()
                        .filter(stat -> stat != null && stat.getUri() != null && stat.getHits() != null)
                        .collect(Collectors.toMap(
                                stat -> extractEventIdFromUri(stat.getUri()),
                                ViewStats::getHits,
                                (h1, h2) -> h1 // если дубликаты, берем первое значение
                        ));
            }
        } catch (Exception e) {
            log.warn("[CompilationService] Ошибка при получении статистики для событий {}: {}",
                    eventIds, e.getMessage());
        }

        return Collections.emptyMap();
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            if (uri == null || uri.isEmpty()) {
                return -1L;
            }

            // Убираем возможные слэши в начале
            String cleanUri = uri.startsWith("/") ? uri.substring(1) : uri;

            // Разбиваем на части
            String[] parts = cleanUri.split("/");

            // Ожидаем формат: events/{id}
            if (parts.length >= 2 && "events".equals(parts[0])) {
                return Long.parseLong(parts[1]);
            } else if (parts.length > 0) {
                // Пробуем достать ID из последней части
                String lastPart = parts[parts.length - 1];
                return Long.parseLong(lastPart);
            }
        } catch (Exception e) {
            log.warn("[CompilationService] Не удалось извлечь ID события из URI: {}", uri);
        }

        return -1L;
    }
}