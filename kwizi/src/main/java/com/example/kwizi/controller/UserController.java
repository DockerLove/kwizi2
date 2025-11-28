package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.*;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.annotations.RateLimited;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.UserService;
import com.example.kwizi.util.TimeUnit;
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
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByUsername(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Поиск пользователя по username: '{}' от пользователя ID: {}",
                username, userDetails.getId());

        UserProfileResponse userProfileResponse = userService.findUsername(username, userDetails.getId());

        logger.info("Пользователь {} найден для пользователя ID: {}",
                username, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Пользователь найден", userProfileResponse));
    }

    @PatchMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyUserEmail(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос верификации email для пользователя ID: {}", userDetails.getId());

        userService.verifyUserEmail(userDetails.getId());

        logger.info("Email успешно подтвержден для пользователя ID: {}", userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Email успешно подтвержден", null));
    }

    @RateLimited(value = 3, timeUnit = TimeUnit.MINUTES, duration = 1)
    @PostMapping("/send-verification-email")
    public ResponseEntity<ApiResponse<String>> sendVerificationEmail(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос отправки verification email для пользователя ID: {}", userDetails.getId());

        authenticationService.sendVerificationEmail(userDetails.getId());

        logger.info("Письмо для подтверждения email отправлено пользователю ID: {}", userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Письмо для подтверждения email отправлено", null));
    }

    @PatchMapping("/bio")
    public ResponseEntity<ApiResponse<String>> updateBio(
            @Valid @RequestBody UpdateBioRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос обновления bio для пользователя ID: {}", userDetails.getId());

        userService.updateBio(userDetails.getId(), request.getBio());

        logger.info("Bio успешно обновлен для пользователя ID: {}", userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Bio успешно обновлен", null));
    }

    @PatchMapping("/firstName")
    public ResponseEntity<ApiResponse<String>> updateFirstName(
            @Valid @RequestBody UpdateFirstNameRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос обновления имени для пользователя ID: {}", userDetails.getId());

        userService.updateFirstName(userDetails.getId(), request.getFirstName());

        logger.info("Имя успешно обновлено для пользователя ID: {}", userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Имя успешно обновлено", null));
    }

    @PatchMapping("/lastName")
    public ResponseEntity<ApiResponse<String>> updateLastName(
            @Valid @RequestBody UpdateLastNameRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос обновления фамилии для пользователя ID: {}", userDetails.getId());

        userService.updateLastName(userDetails.getId(), request.getLastName());

        logger.info("Фамилия успешно обновлена для пользователя ID: {}", userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Фамилия успешно обновлена", null));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос профиля для пользователя ID: {}", userDetails.getId());

        UserProfileResponse profile = userService.getUserProfile(userDetails.getId());

        logger.info("Профиль успешно загружен для пользователя ID: {}", userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Профиль успешно загружен", profile));
    }

    @GetMapping("/verified")
    public ResponseEntity<ApiResponse<Boolean>> getEmailVerified(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос статуса верификации email для пользователя ID: {}", userDetails.getId());

        boolean isVerified = userService.getEmailVerified(userDetails.getId());

        logger.info("Статус верификации email: {} для пользователя ID: {}", isVerified, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Получен статус проверки", isVerified));
    }

    @PatchMapping("/username")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateUsername(
            @Valid @RequestBody UpdateUsernameRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос обновления username для пользователя ID: {}", userDetails.getId());

        String token = userService.updateUsername(userDetails.getId(), request.getUsername());

        logger.info("Username успешно обновлен для пользователя ID: {}", userDetails.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Никнейм успешно обновлен", Map.of("token", token))
        );
    }

    @PatchMapping("/avatar")
    public ResponseEntity<?> updateAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос на изменение фото пользователя ID: {}", userDetails.getId());

        userService.updateUserAvatar(file, userDetails.getId());

        logger.info("Запрос на изменение фото чата ID успешно изменено {}", userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success("Фото пользователя успешно изменено",null));
    }

    @PatchMapping("/email")
    public ResponseEntity<ApiResponse<String>> updateEmail(
            @Valid @RequestBody UpdateEmailRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        logger.info("Запрос обновления фамилии для пользователя ID: {}", userDetails.getId());

        userService.updateEmail(userDetails.getId(), request.getEmail());

        logger.info("Фамилия успешно обновлена для пользователя ID: {}", userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Email успешно обновлен", null));
    }
}