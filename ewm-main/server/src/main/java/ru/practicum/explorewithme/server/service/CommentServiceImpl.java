package ru.practicum.explorewithme.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.comment.dto.CommentDto;
import ru.practicum.explorewithme.server.entity.Comment;
import ru.practicum.explorewithme.server.entity.Event;
import ru.practicum.explorewithme.server.entity.User;
import ru.practicum.explorewithme.server.exception.ConflictException;
import ru.practicum.explorewithme.server.exception.EntityNotFoundException;
import ru.practicum.explorewithme.server.mapper.CommentMapper;
import ru.practicum.explorewithme.server.repository.CommentRepository;
import ru.practicum.explorewithme.server.repository.EventRepository;
import ru.practicum.explorewithme.server.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto create(Long userId, Long eventId, String text) {
        log.info("Создание комментария: userId={}, eventId={}", userId, eventId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие не найдено: " + eventId));

        // Проверяем, опубликовано ли событие
        if (!"PUBLISHED".equals(event.getState().name())) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        Comment comment = commentMapper.toEntity(text, event, author);
        Comment saved = commentRepository.save(comment);

        log.info("✅ Комментарий создан: id={}", saved.getId());
        return commentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long commentId) {
        log.info("Удаление комментария: userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Комментарий не найден: " + commentId));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Нельзя удалить чужой комментарий");
        }

        commentRepository.delete(comment);
        log.info("✅ Комментарий удален: id={}", commentId);
    }

    @Override
    @Transactional
    public void deleteByAdmin(Long commentId) {
        log.info("Админ удаляет комментарий: commentId={}", commentId);

        if (!commentRepository.existsById(commentId)) {
            throw new EntityNotFoundException("Комментарий не найден: " + commentId);
        }

        commentRepository.deleteById(commentId);
        log.info("✅ Комментарий удален администратором: id={}", commentId);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, Integer from, Integer size) {
        log.info("Получение комментариев пользователя: userId={}", userId);

        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Пользователь не найден: " + userId);
        }

        // Временно без пагинации - получаем все комментарии
        List<Comment> comments = commentRepository.findAllByAuthorIdOrderByCreatedAtDesc(userId);

        log.info("Найдено комментариев пользователя {}: {}", userId, comments.size());

        return comments.stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, Integer from, Integer size) {
        log.info("Получение комментариев события: eventId={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие не найдено: " + eventId));

        // Для неопубликованных событий комментарии не показываем
        if (!"PUBLISHED".equals(event.getState().name())) {
            throw new EntityNotFoundException("Событие не найдено");
        }

        // Временно без пагинации - получаем все комментарии
        List<Comment> comments = commentRepository.findAllByEventIdOrderByCreatedAtDesc(eventId);

        log.info("Найдено комментариев для события {}: {}", eventId, comments.size());

        return comments.stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Long getCommentsCount(Long eventId) {
        log.debug("Получение количества комментариев: eventId={}", eventId);
        return commentRepository.countByEventId(eventId);
    }
}