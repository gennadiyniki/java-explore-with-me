package ru.practicum.explorewithme.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.server.entity.Request;
import ru.practicum.explorewithme.server.entity.RequestStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Репозиторий заявок на участие
public interface RequestRepository extends JpaRepository<Request, Long> {

    // Получить все заявки для события
    List<Request> findAllByEventId(Long eventId);

    // Получить заявки пользователя для списка событий
    List<Request> findAllByRequesterIdAndEventIdIn(Long userId, List<Long> eventIds);

    // Получить заявки для события по статусу
    List<Request> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    // Получить все заявки пользователя
    List<Request> findAllByRequesterId(Long userId);

    // Подсчитать количество заявок для события по статусу
    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    // Проверить существование заявки пользователя на событие
    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    @Query("SELECT r.event.id, COUNT(r) " +
            "FROM Request r " +
            "WHERE r.event.id IN :eventIds AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event.id")
    List<Object[]> countConfirmedByEventIds(@Param("eventIds") Collection<Long> eventIds);

    default Map<Long, Long> getConfirmedCountsMap(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> results = countConfirmedByEventIds(eventIds);
        return results.stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    default Map<Long, Long> getConfirmedCountsMap(List<Long> eventIds) {
        return getConfirmedCountsMap((Collection<Long>) eventIds);
    }
}