package ru.practicum.explorewithme.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.server.entity.Compilation;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);

    boolean existsById(Long id);

    @Query(value = "SELECT * FROM compilations ORDER BY id LIMIT :size OFFSET :from",
            nativeQuery = true)
    List<Compilation> findAllWithPagination(@Param("from") int from,
                                            @Param("size") int size);

    @Query(value = "SELECT * FROM compilations WHERE pinned = :pinned ORDER BY id LIMIT :size OFFSET :from",
            nativeQuery = true)
    List<Compilation> findAllByPinned(@Param("pinned") Boolean pinned,
                                      @Param("from") int from,
                                      @Param("size") int size);
}