package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.*;
import com.example.kwizi.DTO.response.ApiResponseDto;
import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.annotations.RateLimited;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.UserService;
import com.example.kwizi.util.TimeUnit;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
@RestController
@RequestMapping("/api/user")
@Validated
@Tag(
        name = "Пользователи",
        description = """
        ### Управление профилем пользователя
        
        Этот модуль предоставляет endpoints для:
        - 👤 Управления персональными данными
        - 📧 Верификации и обновления email
        - 📝 Изменения информации профиля
        - 🖼️ Обновления аватара
        - 🔍 Поиска других пользователей
        
        ### Особенности:
        - Все endpoints требуют JWT аутентификации
        - Данные возвращаются только для текущего пользователя
        - Поддерживается ограничение запросов (rate limiting)
        """
)
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final AuthenticationService authenticationService;
    private final UserService userService;

    @Autowired
    public UserController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @GetMapping("/find/{username}")
    @Operation(
            summary = "Найти пользователя по username",
            description = "Поиск пользователя по имени пользователя. Возвращает публичный профиль."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Пользователь найден",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "❌ Пользователь не найден",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<UserProfileResponse>> getUserByUsername(
            @Parameter(description = "Имя пользователя для поиска", example = "john_doe")
            @PathVariable String username,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Поиск пользователя по username: '{}' от пользователя ID: {}",
                username, userDetails.getId());

        UserProfileResponse userProfileResponse = userService.findUsername(username, userDetails.getId());

        logger.info("Пользователь {} найден для пользователя ID: {}",
                username, userDetails.getId());

        return ResponseEntity.ok(ApiResponseDto.success("Пользователь найден", userProfileResponse));
    }

    @PatchMapping("/verify-email")
    @Operation(
            summary = "Подтвердить email вручную",
            description = "Помечает email пользователя как подтвержденный"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Email успешно подтвержден",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "❌ Email уже подтвержден",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<String>> verifyUserEmail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос верификации email для пользователя ID: {}", userDetails.getId());

        userService.verifyUserEmail(userDetails.getId());

        logger.info("Email успешно подтвержден для пользователя ID: {}", userDetails.getId());

        return ResponseEntity.ok(ApiResponseDto.success("Email успешно подтвержден", null));
    }

    @RateLimited(value = 3, timeUnit = TimeUnit.MINUTES, duration = 1)
    @PostMapping("/send-verification-email")
    @Operation(
            summary = "Отправить письмо подтверждения email",
            description = """
            Отправляет письмо с ссылкой для подтверждения email.
            
            ### Ограничения:
            - Максимум 3 запроса в минуту (rate limited)
            - Письмо отправляется на текущий email пользователя
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Письмо отправлено",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "❌ Слишком много запросов",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "❌ Email уже подтвержден",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<String>> sendVerificationEmail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос отправки verification email для пользователя ID: {}", userDetails.getId());

        authenticationService.sendVerificationEmail(userDetails.getId());

        logger.info("Письмо для подтверждения email отправлено пользователю ID: {}", userDetails.getId());

        return ResponseEntity.ok(ApiResponseDto.success("Письмо для подтверждения email отправлено", null));
    }

    @PatchMapping("/bio")
    @Operation(
            summary = "Обновить биографию",
            description = "Обновляет биографию (о себе) пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Биография обновлена",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректные данные",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<String>> updateBio(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Новая биография пользователя",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateBioRequest.class))
            )
            @Valid @RequestBody UpdateBioRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос обновления bio для пользователя ID: {}", userDetails.getId());

        userService.updateBio(userDetails.getId(), request.getBio());

        logger.info("Bio успешно обновлен для пользователя ID: {}", userDetails.getId());

        return ResponseEntity.ok(ApiResponseDto.success("Bio успешно обновлен", null));
    }

    @PatchMapping("/firstName")
    @Operation(
            summary = "Обновить имя",
            description = "Обновляет имя пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Имя обновлено",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректные данные",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<String>> updateFirstName(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Новое имя пользователя",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateFirstNameRequest.class))
            )
            @Valid @RequestBody UpdateFirstNameRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос обновления имени для пользователя ID: {}", userDetails.getId());

        userService.updateFirstName(userDetails.getId(), request.getFirstName());

        logger.info("Имя успешно обновлено для пользователя ID: {}", userDetails.getId());

        return ResponseEntity.ok(ApiResponseDto.success("Имя успешно обновлено", null));
    }

    @PatchMapping("/lastName")
    @Operation(
            summary = "Обновить фамилию",
            description = "Обновляет фамилию пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Фамилия обновлена",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректные данные",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<String>> updateLastName(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Новая фамилия пользователя",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateLastNameRequest.class))
            )
            @Valid @RequestBody UpdateLastNameRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос обновления фамилии для пользователя ID: {}", userDetails.getId());

        userService.updateLastName(userDetails.getId(), request.getLastName());

        logger.info("Фамилия успешно обновлена для пользователя ID: {}", userDetails.getId());

        return ResponseEntity.ok(ApiResponseDto.success("Фамилия успешно обновлена", null));
    }

    @GetMapping("/profile")
    @Operation(
            summary = "Получить профиль пользователя",
            description = "Возвращает полный профиль текущего пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Профиль загружен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "❌ Пользователь не найден",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<UserProfileResponse>> getUserProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос профиля для пользователя ID: {}", userDetails.getId());

        UserProfileResponse profile = userService.getUserProfile(userDetails.getId());

        logger.info("Профиль успешно загружен для пользователя ID: {}", userDetails.getId());

        return ResponseEntity.ok(ApiResponseDto.success("Профиль успешно загружен", profile));
    }

    @GetMapping("/verified")
    @Operation(
            summary = "Проверить статус верификации email",
            description = "Проверяет, подтвержден ли email пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Статус получен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Boolean>> getEmailVerified(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос статуса верификации email для пользователя ID: {}", userDetails.getId());

        boolean isVerified = userService.getEmailVerified(userDetails.getId());

        logger.info("Статус верификации email: {} для пользователя ID: {}",
                isVerified, userDetails.getId());

        return ResponseEntity.ok(ApiResponseDto.success("Получен статус проверки", isVerified));
    }

    @PatchMapping("/username")
    @Operation(
            summary = "Обновить имя пользователя (username)",
            description = "Изменяет имя пользователя (логин) и возвращает новый JWT токен"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Username обновлен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректные данные",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "❌ Username уже занят",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Map<String, String>>> updateUsername(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Новое имя пользователя",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateUsernameRequest.class))
            )
            @Valid @RequestBody UpdateUsernameRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос обновления username для пользователя ID: {}", userDetails.getId());

        String token = userService.updateUsername(userDetails.getId(), request.getUsername());

        logger.info("Username успешно обновлен для пользователя ID: {}", userDetails.getId());

        return ResponseEntity.ok(
                ApiResponseDto.success("Никнейм успешно обновлен", Map.of("token", token))
        );
    }

    @PatchMapping("/avatar")
    @Operation(
            summary = "Обновить аватар пользователя",
            description = "Загружает новый аватар (фото профиля) пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Аватар обновлен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректный файл",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "413", description = "❌ Файл слишком большой",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<Void>> updateAvatar(
            @Parameter(
                    description = "Файл аватара (изображение)",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data")
            )
            @RequestParam("file") MultipartFile file,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос на изменение фото пользователя ID: {}", userDetails.getId());

        userService.updateUserAvatar(file, userDetails.getId());

        logger.info("Фото пользователя успешно изменено для ID: {}", userDetails.getId());

        return ResponseEntity.ok(ApiResponseDto.success("Фото пользователя успешно изменено", null));
    }

    @PatchMapping("/email")
    @Operation(
            summary = "Обновить email",
            description = "Изменяет email адрес пользователя. Требует повторной верификации."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ Email обновлен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "❌ Некорректный email",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "❌ Не авторизован",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "❌ Email уже используется",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponseDto<String>> updateEmail(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Новый email адрес",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateEmailRequest.class))
            )
            @Valid @RequestBody UpdateEmailRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос обновления email для пользователя ID: {}", userDetails.getId());

        userService.updateEmail(userDetails.getId(), request.getEmail());

        logger.info("Email успешно обновлен для пользователя ID: {}", userDetails.getId());

        return ResponseEntity.ok(ApiResponseDto.success("Email успешно обновлен", null));
    }
}