package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.*;
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


    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return authenticationService.findByUsername(username)
                .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(){
        List<User> users = authenticationService.findAll();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PatchMapping("/verify-email")
    public ResponseEntity<?> verifyUserEmail(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            userService.verifyUserEmail(userDetails.getId());
            return ResponseEntity.ok("Email успешно подтвержден");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    @PostMapping("/send-verification-email")
    public ResponseEntity<String> sendVerificationEmail(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            authenticationService.sendVerificationEmail(userDetails.getId());
            return ResponseEntity.ok("Письмо для подтверждения email отправлено.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при отправке письма: " + e.getMessage());
        }
    }


    @PostMapping("/bio")
    public ResponseEntity<?> updateBio(@Valid @RequestBody UpdateBioRequest request,
                                       BindingResult bindingResult,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (bindingResult.hasErrors()) {
            return buildValidationErrorResponse(bindingResult);
        }
        try {
            userService.updateBio(userDetails.getId(), request.getBio());
            return ResponseEntity.ok("Bio успешно обновлен");
        } catch (UserNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/firstName")
    public ResponseEntity<?> updateFirstName(@Valid @RequestBody UpdateFirstNameRequest request,
                                             BindingResult bindingResult,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (bindingResult.hasErrors()) {
            return buildValidationErrorResponse(bindingResult);
        }
        try {
            userService.updateFirstName(userDetails.getId(), request.getFirstName());
            return ResponseEntity.ok("Имя успешно обновлено");
        } catch (UserNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/lastName")
    public ResponseEntity<?> updateLastName(@Valid @RequestBody UpdateLastNameRequest request,
                                            BindingResult bindingResult,
                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (bindingResult.hasErrors()) {
            return buildValidationErrorResponse(bindingResult);
        }
        try {
            userService.updateLastName(userDetails.getId(), request.getLastName());
            return ResponseEntity.ok("Фамилия успешно обновлена");
        } catch (UserNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
    //TODO спросить по поводу копипасты bindingResult(вынести в private,AOP,Controller Advice)
    @PostMapping("/username")
    public ResponseEntity<?> updateUsername(@Valid @RequestBody UpdateUsernameRequest request,
                                            BindingResult bindingResult,
                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (bindingResult.hasErrors()) {
            return buildValidationErrorResponse(bindingResult);
        }
        try {
            // 1. Обновляем username в базе данных
            userService.updateUsername(userDetails.getId(), request.getUsername());

            // 2. Получаем обновленную информацию о пользователе из базы данных
            Optional<User> updatedUserOptional = userService.findById(userDetails.getId());

            // 3. Проверяем, что пользователь найден
            if (updatedUserOptional.isPresent()) {
                User updatedUser = updatedUserOptional.get();

                // 4. Генерируем новый JWT с новым username
                String token = jwtUtils.generateToken(updatedUser.getUsername());

                // 5. Возвращаем новый JWT клиенту
                return ResponseEntity.ok(Map.of("message", "Никнейм успешно обновлен", "token", token));
            } else {
                // 6. Если пользователь не найден, возвращаем ошибку
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Пользователь не найден");
            }

        } catch (UserNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            UserProfileResponse profile = userService.getUserProfile(userDetails.getId());
            return ResponseEntity.ok(profile);
        }catch(UserNotFoundException ex){
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("verified/{id}")
    public ResponseEntity<?> getEmailVerified(@PathVariable("id")Long id){
        try{
            boolean isVerified = userService.getEmailVerified(id);
            return ResponseEntity.ok(isVerified);
        }catch(UserNotFoundException userNotFoundException){
            userNotFoundException.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<List<String>> buildValidationErrorResponse(BindingResult bindingResult) {
        List<String> errors = bindingResult.getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(errors);
    }
    // Другие методы контроллера (например, для аутентификации, обновления, удаления)
}