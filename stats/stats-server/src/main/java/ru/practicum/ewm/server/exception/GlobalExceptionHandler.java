package ru.practicum.ewm.server.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponse buildErrorResponse(HttpStatus status, Map<String, String> errors,
                                             String message, WebRequest request) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .message(message)
                .path(((ServletWebRequest) request).getRequest().getRequestURI())
                .build();
    }

    private ErrorResponse buildErrorResponse(HttpStatus status, Map<String, String> errors, WebRequest request) {
        return buildErrorResponse(status, errors, null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {

        log.warn(
                "Валидация не пройдена: {} ошибок. Детали: {}",
                ex.getBindingResult().getErrorCount(),
                ex.getMessage(),
                ex
        );

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(err -> {
                    String field = err.getField();
                    if (field.equals("endAfterStart")) {
                        errors.put("end", err.getDefaultMessage());
                    } else {
                        errors.put(field, err.getDefaultMessage());
                    }
                });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, errors, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, WebRequest request) {

        log.warn("Обнаружено нарушение ограничений: {}", ex.getMessage(), ex);

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> {
                            String path = v.getPropertyPath().toString();
                            if (path.equals("endAfterStart")) return "end";
                            return path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                        },
                        v -> v.getMessage(),
                        (existing, replacement) -> existing
                ));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, errors, request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDBIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        log.error("Ошибка базы данных", ex);
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "Нарушение ограничений базы данных: " + ex.getMostSpecificCause().getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, errors, request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {

        log.warn(
                "Несоответствие типа для параметра '{}': значение='{}', ожидаемый тип={}",
                ex.getName(),
                ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "неизвестный",
                ex
        );

        Map<String, String> errors = new HashMap<>();

        if (ex.getRequiredType() == LocalDateTime.class) {
            errors.put("timestamp", "Неверный формат даты. Ожидается: yyyy-MM-dd'T'HH:mm:ss");
        } else {
            errors.put(ex.getName(), "Неверное значение: " + ex.getValue());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, errors, request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                      WebRequest request) {

        log.warn("Некорректный JSON или неправильный формат даты: {}", ex.getMessage(), ex);

        Map<String, String> errors = new HashMap<>();
        errors.put("body", "Неверное тело запроса или формат даты. Ожидается: yyyy-MM-dd'T'HH:mm:ss");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, errors, request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            WebRequest request) {

        log.warn(
                "Отсутствует параметр запроса: '{}' (тип={})",
                ex.getParameterName(),
                ex.getParameterType(),
                ex
        );

        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getParameterName(), "Параметр обязателен для заполнения");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, errors, request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {

        log.warn("Недопустимый аргумент: {}", ex.getMessage(), ex);

        Map<String, String> errors = new HashMap<>();
        errors.put("end", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, errors, ex.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Непредвиденная ошибка", ex);
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "Внутренняя ошибка сервера");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errors,
                        "Внутренняя ошибка сервера", request));
    }
}