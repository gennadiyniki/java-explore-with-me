package ru.practicum.ewm.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewStatsDto {
    private Long hits; // просмотры
    private String app;// Идентификатор
    private String uri;// Путь запроса

}
