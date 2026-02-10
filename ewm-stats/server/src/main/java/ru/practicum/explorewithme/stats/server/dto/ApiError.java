package ru.practicum.explorewithme.stats.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String status;
    private String reason;
    private String message;

    private LocalDateTime timestamp;

    private List<String> errors;
}