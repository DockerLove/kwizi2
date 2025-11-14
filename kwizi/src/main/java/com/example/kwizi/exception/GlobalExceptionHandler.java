package com.example.kwizi.exception;

import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.controller.AuthenticationController;
import com.example.kwizi.controller.ChatController;
import com.example.kwizi.controller.MessageController;
import com.example.kwizi.controller.UserController;
import com.example.kwizi.exception.AuthenticationService.EmailAlreadyVerifiedException;
import com.example.kwizi.exception.AuthenticationService.InvalidPasswordException;
import com.example.kwizi.exception.ChatService.*;
import com.example.kwizi.exception.MessageService.MessageEditTimeExpiredException;
import com.example.kwizi.exception.MessageService.MessageNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = {UserController.class, ChatController.class, AuthenticationController.class, MessageController.class})
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


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

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        logger.warn("Конфликт username: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(),null));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        logger.warn("Конфликт email: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(),null));
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ApiResponse<?>> handleEmailAlreadyVerifiedException(EmailAlreadyVerifiedException ex) {
        logger.warn("Email уже подтвержден: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), null));
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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        logger.warn("Ошибка аутентификации: Неверный логин или пароль");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Неверный логин или пароль",null));
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtAuthentication(JwtAuthenticationException ex) {
        logger.warn("Ошибка JWT аутентификации: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(),null));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPassword(InvalidPasswordException ex) {
        logger.warn("Ошибка при попытке смены пароля: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(),null));
    }

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotFound(MessageNotFoundException ex) {
        logger.warn("Сообщение не найдено: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(),null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("Попытка редактирования или удаления чужого сообщения: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(),null));
    }

    @ExceptionHandler(MessageEditTimeExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleEditTimeExpired(MessageEditTimeExpiredException ex) {
        logger.warn("Попытка редактирования просроченного сообщения: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(),null));
    }

    @ExceptionHandler(ChatMemberNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleUserNotFound(ChatMemberNotFoundException ex) {
        logger.warn("Пользователь не является участником чата: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<?> handleBusinessLogicException(BusinessLogicException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(),null));
    }
    @ExceptionHandler(NotGroupChatException.class)
    public ResponseEntity<?> handleNotGroupChatException(NotGroupChatException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(),null));
    }
    @ExceptionHandler(DuplicateChatMemberException.class)
    public ResponseEntity<?> handleDuplicateChatMemberException(DuplicateChatMemberException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(),null));
    }
    @ExceptionHandler(ChatOperationNotAllowedException.class)
    public ResponseEntity<?> handleChatOperationNotAllowedException(ChatOperationNotAllowedException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(),null));
    }
    @ExceptionHandler(InsufficientPermissionsException.class)
    public ResponseEntity<?> handleInsufficientPermissionsException(InsufficientPermissionsException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(),null));
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<?> handleChatNotFoundException(ChatNotFoundException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(),null));
    }


}