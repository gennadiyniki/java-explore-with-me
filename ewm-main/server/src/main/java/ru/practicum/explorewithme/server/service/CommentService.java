package ru.practicum.explorewithme.server.service;

import ru.practicum.explorewithme.comment.dto.CommentDto;

import java.util.List;

public interface CommentService {
    CommentDto create(Long userId, Long eventId, String text);
    void delete(Long userId, Long commentId);
    void deleteByAdmin(Long commentId);
    List<CommentDto> getUserComments(Long userId, Integer from, Integer size);
    List<CommentDto> getEventComments(Long eventId, Integer from, Integer size);
    Long getCommentsCount(Long eventId);
}