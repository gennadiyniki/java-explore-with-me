package ru.practicum.explorewithme.server.controller.pub;


import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.comment.dto.CommentDto;
import ru.practicum.explorewithme.server.service.CommentService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events/{eventId}/comments")
@RequiredArgsConstructor
@Validated
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getEventComments(@PathVariable @Positive Long eventId,
                                             @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                             @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("[PublicCommentController] GET /events/{}/comments?from={}&size={}", eventId, from, size);
        List<CommentDto> comments = commentService.getEventComments(eventId, from, size);
        log.debug("[PublicCommentController] Найдено {} комментариев для события {}", comments.size(), eventId);
        return comments;
    }

    @GetMapping("/count")
    public Long getCommentsCount(@PathVariable @Positive Long eventId) {
        log.info("[PublicCommentController] GET /events/{}/comments/count", eventId);
        Long count = commentService.getCommentsCount(eventId);
        log.debug("[PublicCommentController] Количество комментариев для события {}: {}", eventId, count);
        return count;
    }
}