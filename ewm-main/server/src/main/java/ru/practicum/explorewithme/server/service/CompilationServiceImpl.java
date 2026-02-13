package ru.practicum.explorewithme.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.compilation.dto.CompilationDto;
import ru.practicum.explorewithme.compilation.dto.NewCompilationDto;
import ru.practicum.explorewithme.compilation.dto.UpdateCompilationRequest;
import ru.practicum.explorewithme.event.dto.EventShortDto;
import ru.practicum.explorewithme.server.entity.Compilation;
import ru.practicum.explorewithme.server.entity.Event;
import ru.practicum.explorewithme.server.exception.EntityNotFoundException;
import ru.practicum.explorewithme.server.mapper.CompilationMapper;
import ru.practicum.explorewithme.server.mapper.EventMapper;
import ru.practicum.explorewithme.server.repository.CompilationRepository;
import ru.practicum.explorewithme.server.repository.EventRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CompilationMapper compilationMapper;
    private final EventService eventService;
    private final CommentService commentService;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        log.info("Создание новой подборки: {}", newCompilationDto.getTitle());

        Set<Event> events = new HashSet<>();
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));
        }

        Compilation compilation = Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false)
                .events(events)
                .build();

        compilation = compilationRepository.save(compilation);
        log.info("Подборка создана: id={}, title={}", compilation.getId(), compilation.getTitle());

        return toCompilationDto(compilation);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest update) {
        log.info("Обновление подборки: id={}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new EntityNotFoundException("Подборка не найдена: " + compId));

        if (update.getTitle() != null && !update.getTitle().trim().isEmpty()) {
            compilation.setTitle(update.getTitle());
        }

        if (update.getPinned() != null) {
            compilation.setPinned(update.getPinned());
        }

        if (update.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(update.getEvents()));
            compilation.setEvents(events);
        }

        compilation = compilationRepository.save(compilation);
        log.info("Подборка обновлена: id={}", compilation.getId());

        return toCompilationDto(compilation);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        log.info("Удаление подборки: id={}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new EntityNotFoundException("Подборка не найдена: " + compId);
        }

        compilationRepository.deleteById(compId);
        log.info("Подборка удалена: id={}", compId);
    }

    @Override
    public CompilationDto getById(Long compId) {
        log.info("Получение подборки: id={}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new EntityNotFoundException("Подборка не найдена: " + compId));

        return toCompilationDto(compilation);
    }

    @Override
    public List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size) {
        log.info("Получение всех подборок: pinned={}, from={}, size={}", pinned, from, size);

        int safeFrom = from != null ? from : 0;
        int safeSize = size != null ? size : 10;

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, safeFrom, safeSize);
        } else {
            compilations = compilationRepository.findAllWithPagination(safeFrom, safeSize);
        }

        return compilations.stream()
                .map(this::toCompilationDto)
                .collect(Collectors.toList());
    }

    private CompilationDto toCompilationDto(Compilation compilation) {
        List<EventShortDto> eventShortDtos = compilation.getEvents().stream()
                .map(event -> {
                    Long confirmedRequests = eventService.getConfirmedCount(event.getId());
                    Long views = eventService.getViewsForEvent(event.getId());
                    Long commentCount = commentService.getCommentsCount(event.getId());
                    return eventMapper.toShortDto(event, confirmedRequests, views, commentCount);
                })
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventShortDtos)
                .build();
    }
}