package ru.practicum.ewm.stats.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StatsRequestDto {

    @NotNull(message = "Дата начала обязательна для заполнения")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime start;

    @NotNull(message = "Дата окончания обязательна для заполнения")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime end;

    private List<String> uris;

    @Builder.Default
    private Boolean unique = false;

    @AssertTrue(message = "Дата окончания должна быть позже даты начала")
    public boolean isEndAfterStart() {
        return end.isAfter(start);  // Простая проверка, @NotNull уже отловит null
    }

    public boolean isUnique() {
        return Boolean.TRUE.equals(unique);
    }
}