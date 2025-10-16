package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.UpdateBioRequest;
import com.example.kwizi.DTO.request.UpdateFirstNameRequest;
import com.example.kwizi.DTO.request.UpdateLastNameRequest;
import com.example.kwizi.DTO.request.UpdateUsernameRequest;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.annotations.RateLimited;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.security.JwtUtils;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.UserService;
import com.example.kwizi.util.TimeUnit;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private AuthenticationService authenticationService;

    private UserService userService;

    private JwtUtils jwtUtils;



    @Autowired
    public UserController(AuthenticationService authenticationService, UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.jwtUtils = jwtUtils;
    }



    @GetMapping("/find/{username}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByUsername(@PathVariable String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        UserProfileResponse userProfileResponse = new UserProfileResponse(user.getId(),user.getFirstName(), user.getLastName(),user.getUsername(),user.getBio(),user.getEmail());
        return ResponseEntity.ok(
                ApiResponse.success("Пользователь найден", userProfileResponse)
        );
    }


    @PatchMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyUserEmail(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        userService.verifyUserEmail(userDetails.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Email успешно подтвержден", null)
        );
    }

    @RateLimited(value = 3, timeUnit = TimeUnit.MINUTES, duration = 1)
    @PostMapping("/send-verification-email")
    public ResponseEntity<ApiResponse<String>> sendVerificationEmail(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        authenticationService.sendVerificationEmail(userDetails.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Письмо для подтверждения email отправлено", null)
        );
    }


    @PostMapping("/bio")
    public ResponseEntity<?> updateBio(@Valid @RequestBody UpdateBioRequest request,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.updateBio(userDetails.getId(), request.getBio());
        return ResponseEntity.ok(
                ApiResponse.success("Bio успешно обновлен", null)
        );
    }

    @PostMapping("/firstName")
    public ResponseEntity<?> updateFirstName(@Valid @RequestBody UpdateFirstNameRequest request,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.updateFirstName(userDetails.getId(), request.getFirstName());
        return ResponseEntity.ok(
                ApiResponse.success("Имя успешно обновлено", null)
        );
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        UserProfileResponse profile = userService.getUserProfile(userDetails.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Профиль успешно загружен", profile)
        );
    }

    @GetMapping("/verified")
    public ResponseEntity<ApiResponse<Boolean>> getEmailVerified(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        boolean isVerified = userService.getEmailVerified(userDetails.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Получен статус проверки", isVerified)
        );
    }
    @PostMapping("/lastName")
    public ResponseEntity<ApiResponse<String>> updateLastName(
            @Valid @RequestBody UpdateLastNameRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        userService.updateLastName(userDetails.getId(), request.getLastName());
        return ResponseEntity.ok(ApiResponse.success("Фамилия успешно обновлена", null));
    }


    @PostMapping("/username")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateUsername(
            @Valid @RequestBody UpdateUsernameRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String token = userService.updateUsername(userDetails.getId(), request.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Никнейм успешно обновлен",
                        Map.of("token", token)
                )
        );
    }

}