package ru.practicum.explorewithme.server.controller.priv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.comment.dto.CommentDto;
import ru.practicum.explorewithme.comment.dto.NewCommentDto;
import ru.practicum.explorewithme.server.service.CommentService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
@Validated
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(@PathVariable @Positive Long userId,
                             @RequestParam @Positive Long eventId,
                             @Valid @RequestBody NewCommentDto newComment) {
        log.info("[PrivateCommentController] POST /users/{}/comments?eventId={}", userId, eventId);
        return commentService.create(userId, eventId, newComment.getText());
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long userId,
                       @PathVariable @Positive Long commentId) {
        log.info("[PrivateCommentController] DELETE /users/{}/comments/{}", userId, commentId);
        commentService.delete(userId, commentId);
    }

    @GetMapping
    public List<CommentDto> getUserComments(@PathVariable @Positive Long userId,
                                            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                            @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("[PrivateCommentController] GET /users/{}/comments?from={}&size={}", userId, from, size);
        return commentService.getUserComments(userId, from, size);
    }
}