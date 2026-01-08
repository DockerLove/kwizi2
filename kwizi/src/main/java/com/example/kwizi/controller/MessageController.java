package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.EditMessageRequest;
import com.example.kwizi.DTO.response.ApiResponseDto;
import com.example.kwizi.DTO.response.ChatHistoryResponse;
import com.example.kwizi.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/messages")
@Tag(
        name = "Сообщения",
        description = """
        ### Управление сообщениями в чатах
        
        Этот модуль предоставляет endpoints для:
        - 📜 Получения истории сообщений в чате
        - ✏️ Редактирования отправленных сообщений
        - 🗑️ Удаления сообщений
        
        ### Особенности:
        - Пагинация для истории сообщений
        - Редактирование доступно только автору сообщения
        - Удаление сообщений с проверкой прав доступа
        - Сортировка сообщений по времени
        """
)
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final ChatMessageService chatMessageService;
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    public MessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/history/{chatId}")
    @Operation(
            summary = "Получить историю сообщений чата",
            description = """
            Возвращает историю сообщений указанного чата с поддержкой пагинации и сортировки.
            
            ### Пагинация:
            - Использует Spring Data Pageable
            - По умолчанию: страница 0, 50 сообщений на странице
            - Сортировка по умолчанию: по дате создания (новые сначала)
            
            ### Сортировка:
            Поддерживает сортировку по полям:
            - `createdAt` - дата создания (asc/desc)
            - `id` - ID сообщения
            
            ### Пример запроса:
            ```
            GET /api/messages/history/1?page=0&size=20&sort=createdAt,desc
            ```
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ История сообщений успешно получена",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "❌ Некорректные параметры запроса",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "❌ Нет доступа к этому чату",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "❌ Чат не найден",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<ApiResponseDto<Page<ChatHistoryResponse>>> getChatHistory(
            @Parameter(
                    description = "ID чата",
                    example = "1",
                    required = true
            )
            @PathVariable Long chatId,

            @Parameter(
                    description = "Номер страницы (начинается с 0)",
                    example = "0",
                    required = false
            )
            @RequestParam(defaultValue = "0") int page,

            @Parameter(
                    description = "Количество сообщений на странице",
                    example = "50",
                    required = false
            )
            @RequestParam(defaultValue = "50") int size,

            @Parameter(
                    description = "Параметры сортировки (поле,направление)",
                    example = "createdAt,desc",
                    required = false
            )
            @RequestParam(defaultValue = "createdAt,desc") String sort,

            @Parameter(hidden = true)
            Authentication authentication) {

        logger.info("Запрос истории чата. ID чата: {}, страница: {}, размер: {}, пользователь: {}",
                chatId, page, size, authentication.getName());

        Page<ChatHistoryResponse> messages = chatMessageService.getChatHistory(
                chatId, page, size, sort, authentication.getName());

        logger.info("История чата успешно получена. ID чата: {}, сообщений на странице: {}",
                chatId, messages.getNumberOfElements());

        return ResponseEntity.ok(ApiResponseDto.success("История чата успешно загружена", messages));
    }

    @PatchMapping("/edit/{messageId}")
    @Operation(
            summary = "Отредактировать сообщение",
            description = """
            Редактирует текст существующего сообщения.
            
            ### Ограничения:
            - Редактировать можно только свои сообщения
            - Сообщение должно быть не старше 15 минут (если настроено)
            - В истории сохраняется отметка об редактировании
            
            ### Использование:
            Изменяет только текст сообщения, остальные данные остаются прежними.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ Сообщение успешно отредактировано",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "❌ Некорректные данные запроса",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "❌ Нет прав для редактирования этого сообщения",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "❌ Сообщение не найдено",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "❌ Время редактирования истекло (15 минут)",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<ApiResponseDto<Void>> editMessage(
            @Parameter(
                    description = "ID сообщения для редактирования",
                    example = "123",
                    required = true
            )
            @PathVariable Long messageId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Новый текст сообщения",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EditMessageRequest.class))
            )
            @Valid @RequestBody EditMessageRequest request,

            @Parameter(hidden = true)
            Authentication authentication) {

        String username = authentication.getName();

        logger.info("Запрос на редактирование сообщения. Message ID: {}, User: {}",
                messageId, username);

        chatMessageService.editMessage(messageId, request.getText(), username);

        logger.info("Сообщение успешно отредактировано. Message ID: {}, User: {}",
                messageId, username);

        return ResponseEntity.ok(ApiResponseDto.success("Сообщение изменено", null));
    }

    @DeleteMapping("/delete/{messageId}")
    @Operation(
            summary = "Удалить сообщение",
            description = """
            Удаляет сообщение из чата.
            
            ### Ограничения:
            - Удалять можно только свои сообщения
            - Администраторы могут удалять любые сообщения в групповых чатах
            - В приватных чатах только автор может удалять свои сообщения
            
            ### Примечание:
            Удаление может быть "мягким" (помечается как удаленное) 
            или "жестким" (полное удаление из БД) в зависимости от настроек.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ Сообщение успешно удалено",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "❌ Нет прав для удаления этого сообщения",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "❌ Сообщение не найдено",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteMessage(
            @Parameter(
                    description = "ID сообщения для удаления",
                    example = "123",
                    required = true
            )
            @PathVariable Long messageId,

            @Parameter(hidden = true)
            Authentication authentication) {

        String username = authentication.getName();

        logger.info("Запрос на удаление сообщения. Message ID: {}, User: {}",
                messageId, username);

        chatMessageService.deleteMessage(messageId, username);

        logger.info("Сообщение успешно удалено. Message ID: {}, User: {}",
                messageId, username);

        return ResponseEntity.ok(ApiResponseDto.success("Сообщение успешно удалено", null));
    }
}