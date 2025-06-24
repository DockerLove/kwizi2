package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.*;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;

import com.example.kwizi.security.JwtUtils;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.RegistrationService;
import com.example.kwizi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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


    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        // 1. Получаем имя пользователя из контекста Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName(); // Получаем имя пользователя из JWT

        try {
            authenticationService.changePassword(username, request);
            return ResponseEntity.ok("Password changed successfully");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid old password");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }


    @GetMapping("/find/{username}")
    public ResponseEntity<ApiResponse<User>> getUserByUsername(@PathVariable String username) {
        User user = authenticationService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        return ResponseEntity.ok(
                ApiResponse.success("Пользователь найден", user)
        );
    }


    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(){
        List<User> users = authenticationService.findAll();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PatchMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyUserEmail(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        userService.verifyUserEmail(userDetails.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Email успешно подтвержден", null)
        );
    }

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

    //TODO спросить по поводу копипасты bindingResult(вынести в private,AOP,Controller Advice)

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