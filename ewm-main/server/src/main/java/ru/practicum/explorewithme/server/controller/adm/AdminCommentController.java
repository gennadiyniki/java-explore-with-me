package ru.practicum.explorewithme.server.controller.adm;


import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.server.service.CommentService;

@Slf4j
@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
public class AdminCommentController {

    private final CommentService commentService;

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable @Positive Long commentId) {
        log.info("[AdminCommentController] DELETE /admin/comments/{}", commentId);
        commentService.deleteByAdmin(commentId);
        log.info("[AdminCommentController] Комментарий {} удален администратором", commentId);
    }
}