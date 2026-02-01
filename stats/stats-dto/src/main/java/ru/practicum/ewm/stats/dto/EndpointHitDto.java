package ru.practicum.ewm.stats.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EndpointHitDto {

    private Long id;

    @NotBlank(message = "Идентификатор сервиса обязателен")
    @Size(max = 255, message = "Длина идентификатора сервиса не должна превышать 255 символов")
    private String app;

    @NotBlank(message = "URI обязателен")
    @Size(max = 2048, message = "Длина URI не должна превышать 2048 символов")
    private String uri;

    @NotBlank(message = "IP-адрес обязателен")
    @Pattern(
            regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
            message = "Неверный формат IPv4 адреса. Пример: 192.168.1.1"
    )
    private String ip;

    @NotNull(message = "Временная метка обязательна")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}