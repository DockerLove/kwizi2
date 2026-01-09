package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.DTO.request.UpdateGroupNameRequest;
import com.example.kwizi.DTO.response.ApiResponseDto;
import com.example.kwizi.DTO.response.ChatPreviewResponse;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.ChatService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api/chats")
@Tag(
        name = "Чаты",
        description = """
        ### Управление чатами
        
        Этот модуль предоставляет endpoints для:
        - 💬 Создания приватных и групповых чатов
        - 👥 Управления участниками чатов
        - 🛠️ Назначения администраторов
        - 📝 Изменения настроек чатов
        - 📄 Получения списка чатов пользователя
        
        ### Типы чатов:
        - **Приватные чаты** - между двумя пользователями
        - **Групповые чаты** - с несколькими участниками и администраторами
        """
)
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/private")
    @Operation(
            summary = "Создать приватный чат",
            description = "Создает приватный чат между текущим пользователем и указанным получателем"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Приватный чат успешно создан",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректные данные запроса",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Недостаточно прав",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "❌ Получатель не найден",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "❌ Чат уже существует",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Void>> createPrivateChat(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для создания приватного чата",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreatePrivateChatRequest.class))
            )
            @Valid @RequestBody CreatePrivateChatRequest createPrivateChatRequest,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String username = userDetails.getUsername();
        logger.info("Запрос на создание приватного чата. Инициатор: {}, получатель: {}",
                username, createPrivateChatRequest.getRecipientUsername());

        chatService.createPrivateChat(createPrivateChatRequest, username);

        logger.info("Приватный чат успешно создан. Инициатор: {}, получатель: {}",
                username, createPrivateChatRequest.getRecipientUsername());

        return ResponseEntity.ok(ApiResponseDto.success("Приватный чат успешно создан", null));
    }

    @PostMapping("/group")
    @Operation(
            summary = "Создать групповой чат",
            description = "Создает групповой чат с несколькими участниками"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Групповой чат успешно создан",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректные данные запроса",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Недостаточно прав",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Void>> createGroupChat(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для создания группового чата",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateGroupChatRequest.class))
            )
            @Valid @RequestBody CreateGroupChatRequest createChatRequestDto,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String username = userDetails.getUsername();
        logger.info("Запрос на создание группового чата. Инициатор: {}, название чата: {}",
                username, createChatRequestDto.getGroupName());

        chatService.createGroupChat(createChatRequestDto, username);

        logger.info("Групповой чат успешно создан. Название чата: {}, Инициатор: {}",
                createChatRequestDto.getGroupName(), username);

        return ResponseEntity.ok(ApiResponseDto.success("Групповой чат успешно создан", null));
    }

    @PostMapping("/{chatId}/members")
    @Operation(
            summary = "Добавить участника в чат",
            description = "Добавляет нового участника в существующий чат"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Участник успешно добавлен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректные данные запроса",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Недостаточно прав",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "❌ Чат или пользователь не найден",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "❌ Участник уже в чате",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Void>> addChatMember(
            @Parameter(description = "ID чата", example = "1")
            @PathVariable Long chatId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для добавления участника",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AddChatMemberRequestDto.class))
            )
            @Valid @RequestBody AddChatMemberRequestDto addChatMemberRequestDto,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String username = userDetails.getUsername();
        logger.info("Запрос на добавление участника в чат. ID чата: {}, ID пользователя для добавления: {}",
                chatId, addChatMemberRequestDto.getUserId());

        addChatMemberRequestDto.setChatId(chatId);
        chatService.addChatMember(addChatMemberRequestDto, username);

        logger.info("Пользователь добавлен в чат. ID чата: {}, ID добавленного пользователя: {}",
                chatId, addChatMemberRequestDto.getUserId());

        return ResponseEntity.ok(ApiResponseDto.success("Пользователь успешно добавлен в чат", null));
    }

    @PatchMapping("/{chatId}/members/{userId}/admin")
    @Operation(
            summary = "Назначить администратора",
            description = "Назначает пользователя администратором чата"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Администратор успешно назначен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Недостаточно прав",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "❌ Чат или пользователь не найден",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Void>> setAdmin(
            @Parameter(description = "ID чата", example = "1")
            @PathVariable Long chatId,

            @Parameter(description = "ID пользователя", example = "2")
            @PathVariable Long userId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Long currentUserId = currentUser.getId();
        logger.info("Запрос на назначение пользователя админом. ID чата: {}, ID пользователя: {}, инициатор: {}",
                chatId, userId, currentUserId);

        chatService.setAdmin(chatId, userId, currentUserId);

        logger.info("Пользователь успешно назначен админом. ID чата: {}, ID пользователя: {}, инициатор: {}",
                chatId, userId, currentUserId);

        return ResponseEntity.ok(ApiResponseDto.success("Пользователь успешно назначен администратором", null));
    }

    @DeleteMapping("/{chatId}/members/{id}")
    @Operation(
            summary = "Удалить участника из чата",
            description = "Удаляет участника из чата (только для администраторов)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Участник успешно удален",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Недостаточно прав",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "❌ Чат или участник не найден",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Void>> removeChatMember(
            @Parameter(description = "ID чата", example = "1")
            @PathVariable Long chatId,

            @Parameter(description = "ID удаляемого пользователя", example = "3")
            @PathVariable Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Long currentUserId = currentUser.getId();
        logger.info("Запрос на удаление участника из чата. ID чата: {}, ID удаляемого пользователя: {}, инициатор: {}",
                chatId, id, currentUserId);

        chatService.removeChatMember(chatId, id, currentUserId);

        logger.info("Пользователь удален из чата. ID чата: {}, ID удаленного пользователя: {}, инициатор: {}",
                chatId, id, currentUserId);

        return ResponseEntity.ok(ApiResponseDto.success("Пользователь удален из чата", null));
    }

    @DeleteMapping("/{chatId}/members/me")
    @Operation(
            summary = "Выйти из чата",
            description = "Позволяет текущему пользователю выйти из чата"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Успешно вышел из чата",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Недостаточно прав",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "❌ Чат не найден",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Void>> leaveChat(
            @Parameter(description = "ID чата", example = "1")
            @PathVariable Long chatId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Long currentUserId = currentUser.getId();
        logger.info("Запрос на выход из чата. ID чата: {}, ID пользователя: {}", chatId, currentUserId);

        chatService.leaveChat(chatId, currentUserId);

        logger.info("Пользователь вышел из чата. ID чата: {}, ID пользователя: {}", chatId, currentUserId);

        return ResponseEntity.ok(ApiResponseDto.success("Вы вышли из чата", null));
    }

    @PatchMapping("/{chatId}/members/{userId}/demote")
    @Operation(
            summary = "Разжаловать администратора",
            description = "Снимает права администратора с пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Администратор успешно разжалован",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Недостаточно прав",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "❌ Чат или пользователь не найден",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Void>> demoteAdminToMember(
            @Parameter(description = "ID чата", example = "1")
            @PathVariable Long chatId,

            @Parameter(description = "ID пользователя", example = "2")
            @PathVariable Long userId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Long currentUserId = currentUser.getId();
        logger.info("Запрос на разжалование администратора. ID чата: {}, ID пользователя: {}, инициатор: {}",
                chatId, userId, currentUserId);

        chatService.demoteAdminToMember(chatId, userId, currentUserId);

        logger.info("Администратор разжалован. ID чата: {}, ID пользователя: {}, инициатор: {}",
                chatId, userId, currentUserId);

        return ResponseEntity.ok(ApiResponseDto.success("Администратор разжалован до обычного участника", null));
    }

    @PatchMapping("/{chatId}/group-name")
    @Operation(
            summary = "Изменить название группы",
            description = "Изменяет название группового чата"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Название группы успешно изменено",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректное название",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Недостаточно прав",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "❌ Чат не найден",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<String>> updateGroupName(
            @Parameter(description = "ID чата", example = "1")
            @PathVariable Long chatId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Новое название группы",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateGroupNameRequest.class))
            )
            @Valid @RequestBody UpdateGroupNameRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long userId = userDetails.getId();
        chatService.updateGroupName(chatId, request.getGroupName(), userId);

        return ResponseEntity.ok(ApiResponseDto.success("Название группы успешно изменено", request.getGroupName()));
    }

    @PatchMapping(value = "/{chatId}/avatar",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Изменить аватар чата",
            description = "Загружает новый аватар для чата"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Аватар успешно изменен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректный файл",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Недостаточно прав",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "❌ Чат не найден",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "413", description = "❌ Файл слишком большой",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Void>> updateAvatar(
            @Parameter(description = "ID чата", example = "1")
            @PathVariable Long chatId,

            @Parameter(
                    description = "Файл аватара (изображение)",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data")
            )
            @RequestPart("file") MultipartFile file,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос на изменение фото чата ID: {}", chatId);

        chatService.updateChatAvatar(chatId, file, userDetails.getId());

        logger.info("Запрос на изменение фото чата ID успешно изменено {}", chatId);

        return ResponseEntity.ok(ApiResponseDto.success("Фото группы успешно изменено", null));
    }

    @GetMapping()
    @Operation(
            summary = "Получить чаты пользователя",
            description = "Возвращает список чатов текущего пользователя с пагинацией"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Список чатов успешно получен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Недостаточно прав",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректные параметры пагинации",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Page<ChatPreviewResponse>>> getUserChats(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails,

            @Parameter(
                    description = "Количество чатов на странице",
                    example = "50",
                    required = false
            )
            @RequestParam(defaultValue = "50") int size,

            @Parameter(
                    description = "Номер страницы (начинается с 0)",
                    example = "0",
                    required = false
            )
            @RequestParam(defaultValue = "0") int page) {

        Page<ChatPreviewResponse> chats = chatService.getUserChatsPreview(userDetails.getId(), page, size);

        return ResponseEntity.ok(ApiResponseDto.success("Самые новые чаты успешно загружены", chats));
    }
}