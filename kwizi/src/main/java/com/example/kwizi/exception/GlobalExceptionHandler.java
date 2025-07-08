package com.example.kwizi.exception;

import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.controller.ChatController;
import com.example.kwizi.controller.UserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = {UserController.class, ChatController.class})
public class GlobalExceptionHandler {

    // Обработка ошибок валидации (@Valid)
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Обработка ошибок валидации (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        logger.warn("Ошибка валидации: {}", errors); // Логируем ошибки валидации

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Ошибка валидации", errors));
    }

    // Обработка UserNotFoundException
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleUserNotFound(UserNotFoundException ex) {
        logger.warn("Пользователь не найден: {}", ex.getMessage()); // Логируем UserNotFoundException
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), null));
    }

    // Обработка IllegalArgumentException (например, для дубликатов username)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Неверный аргумент: {}", ex.getMessage()); // Логируем IllegalArgumentException
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(), null));
    }

    // Общая обработка других исключений (опционально)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleOtherExceptions(Exception ex) {
        logger.error("Внутренняя ошибка сервера: {}", ex.getMessage(), ex); // Логируем другие исключения, включая stack trace
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Internal server error", null));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleRateLimitError(RateLimitExceededException ex) {
        logger.warn("Превышен лимит запросов: {}", ex.getMessage()); // Логируем ошибку превышения лимита
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(ex.getMessage(), null));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalState(IllegalStateException ex){
        logger.warn("Ошибка состояния: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage(), null));
    }
}