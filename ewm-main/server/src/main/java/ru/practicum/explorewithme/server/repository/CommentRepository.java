package ru.practicum.explorewithme.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explorewithme.server.entity.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long countByEventId(Long eventId);

    // Самые простые методы — без @Query, без Pageable
    List<Comment> findAllByEventIdOrderByCreatedAtDesc(Long eventId);

    List<Comment> findAllByAuthorIdOrderByCreatedAtDesc(Long authorId);
}