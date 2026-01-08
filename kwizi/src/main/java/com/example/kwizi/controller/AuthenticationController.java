package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.AuthenticationRequest;
import com.example.kwizi.DTO.request.ChangePasswordRequest;
import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.DTO.response.ApiResponseDto;
import com.example.kwizi.DTO.response.AuthenticationResponse;
import com.example.kwizi.security.JwtUtils;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.RegistrationService;
import com.example.kwizi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Аутентификация и Авторизация",
        description = """
        ### Управление аутентификацией пользователей
        
        Этот модуль предоставляет endpoints для:
        - 📝 Регистрации новых пользователей
        - 🔐 Входа в систему и получения JWT токена
        - 🔄 Смены пароля
        - 🚪 Выхода из системы (инвалидация токена)
        
        ### Поток работы:
        1. Зарегистрируйтесь через `/api/auth/register`
        2. Войдите через `/api/auth/login` для получения JWT токена
        3. Используйте токен в заголовке: `Authorization: Bearer <ваш-токен>`
        4. Для выхода используйте `/api/auth/logout`
        """
)
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationManager authenticationManager;
    private final AuthenticationService authenticationService;
    private final UserDetailsService userDetailsService;
    private final RegistrationService registrationService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @Autowired
    public AuthenticationController(
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtUtils jwtUtils,
            RegistrationService registrationService,
            AuthenticationService authenticationService,
            UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @PostMapping("/password")
    @Operation(
            summary = "Смена пароля",
            description = """
            Позволяет авторизованному пользователю изменить свой пароль.
            
            ### Требования:
            - Пользователь должен быть аутентифицирован (JWT токен)
            - Новый пароль должен соответствовать политике безопасности
            
            ### Безопасность:
            - Старый пароль проверяется
            - Новый пароль хешируется перед сохранением
            - Все активные сессии могут быть инвалидированы
            """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ Пароль успешно изменен",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "❌ Некорректные данные или пароль не соответствует требованиям",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "❌ Неавторизованный доступ - требуется JWT токен",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<ApiResponseDto<Void>> changePassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для смены пароля",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChangePasswordRequest.class))
            )
            @Valid @RequestBody ChangePasswordRequest request,

            @Parameter(
                    description = "Детали авторизованного пользователя (автоматически извлекается из JWT токена)",
                    hidden = true
            )
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос смены пароля для пользователя: {}", userDetails.getUsername());
        authenticationService.changePassword(userDetails.getUsername(), request);
        logger.info("Пароль успешно изменен для пользователя: {}", userDetails.getUsername());

        return ResponseEntity.ok(ApiResponseDto.success("Пароль успешно изменен", null));
    }

    @PostMapping("/register")
    @Operation(
            summary = "Регистрация нового пользователя",
            description = """
            Создает нового пользователя в системе.
            
            ### Процесс регистрации:
            1. Проверка уникальности email/username
            2. Валидация данных
            3. Хеширование пароля
            4. Сохранение пользователя в БД
            5. Отправка email для подтверждения (если настроено)
            
            ### Требования к данным:
            - Email должен быть уникальным и валидным
            - Пароль должен соответствовать политике безопасности
            - Все обязательные поля должны быть заполнены
            """,
            security = {}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ Пользователь успешно зарегистрирован",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                ❌ Некорректные данные
                """,
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "❌ Пользователь с таким email/username уже существует",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "❌ Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<ApiResponseDto<Void>> registerUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для регистрации нового пользователя",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RegistrationRequest.class))
            )
            @Valid @RequestBody RegistrationRequest registrationRequest) {

        logger.info("Запрос регистрации пользователя: {}", registrationRequest.getUsername());
        registrationService.registerUser(registrationRequest);
        logger.info("Пользователь успешно зарегистрирован: {}", registrationRequest.getUsername());

        return ResponseEntity.ok(ApiResponseDto.success("Пользователь успешно зарегистрирован", null));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Аутентификация пользователя",
            description = """
            Аутентифицирует пользователя и возвращает JWT токен для доступа к защищенным ресурсам.
            
            ### Процесс:
            1. Проверка учетных данных
            2. Генерация JWT токена
            3. Возврат токена в ответе
            
            ### Использование токена:
            Добавьте в заголовок запроса:
            ```
            Authorization: Bearer <ваш-jwt-токен>
            ```
            
            ### Время жизни токена:
            Токен действителен 24 часа (настраивается в конфигурации)
            """,
            security = {}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ Успешная аутентификация",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "❌ Некорректный запрос",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "❌ Неверные учетные данные (неправильный логин или пароль)",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<AuthenticationResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Учетные данные для входа",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AuthenticationRequest.class))
            )
            @Valid @RequestBody AuthenticationRequest authenticationRequest) {

        String username = authenticationRequest.getUsername();
        logger.info("Запрос аутентификации пользователя: {}", username);

        logger.debug("Попытка аутентификации для пользователя: {}", username);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, authenticationRequest.getPassword()));

        logger.debug("Загрузка UserDetails для пользователя: {}", username);
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        logger.debug("Генерация JWT токена для пользователя: {}", username);
        final String jwt = jwtUtils.generateToken(
                userDetails.getUsername(),
                userService.findByUsername(userDetails.getUsername()).get().getId()
        );

        logger.info("Успешная аутентификация пользователя: {}", username);
        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Выход из системы",
            description = """
            Инвалидирует JWT токен пользователя.
            
            ### Что происходит:
            1. Токен добавляется в черный список
            2. Все последующие запросы с этим токеном будут отклонены
            3. Пользователю нужно снова войти через /api/auth/login
            
            ### Примечание:
            - Токен извлекается из заголовка Authorization
            - После logout токен становится недействительным немедленно
            """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ Успешный выход из системы",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "❌ Токен отсутствует или некорректен",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "❌ Невалидный JWT токен",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "❌ Неавторизованный доступ",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "❌ Ошибка при обработке logout",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<ApiResponseDto<Void>> logout(
            @Parameter(
                    description = "HTTP запрос для извлечения JWT токена",
                    hidden = true
            )
            HttpServletRequest request) {

        logger.info("Запрос выхода из системы");

        String token = jwtUtils.extractToken(request);
        if (token == null) {
            logger.warn("Попытка выхода без токена");
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("Токен отсутствует", null));
        }

        authenticationService.logout(token);
        logger.info("Успешный выход пользователя");

        return ResponseEntity.ok(ApiResponseDto.success("Выход выполнен успешно", null));
    }

    @GetMapping("/health")
    @Operation(
            summary = "Проверка состояния сервиса",
            description = """
    Healthcheck endpoint для мониторинга работоспособности приложения.
    
    ### Что проверяется:
    1. Доступность Spring Boot приложения
    2. Корректность работы веб-сервера
    3. Готовность к обработке HTTP запросов
    
    ### Использование:
    - **Docker Healthcheck** - автоматическая проверка состояния контейнера
    - **CI/CD пайплайны** - проверка готовности перед тестами
    - **Мониторинг** - внешние системы проверки доступности
    - **Балансировщики нагрузки** - проверка health-статуса инстансов
    
    ### Технические детали:
    - Endpoint всегда возвращает HTTP 200 OK при рабочем состоянии
    - Ответ содержит простую текстовую строку "OK"
    - Не требует аутентификации
    - Минимальная нагрузка на систему
    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = """
        ✅ Сервис работает корректно
        
        **Пример ответа:**
        ```
        OK
        ```
        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "string", example = "OK"),
                            examples = @ExampleObject(value = "OK")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = """
        ❌ Внутренняя ошибка сервиса
        
        **Возможные причины:**
        - Сервис не запущен
        - Критическая ошибка в приложении
        - Проблемы с зависимостями
        - Недоступность внутренних компонентов
        """,
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = """
        ⚠️ Сервис временно недоступен
        
        **Возможные причины:**
        - Техническое обслуживание
        - Высокая нагрузка
        - Зависимые сервисы недоступны
        """,
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<ApiResponseDto<Void>> health() {
        return ResponseEntity.ok(ApiResponseDto.success("OK",null));
    }
}