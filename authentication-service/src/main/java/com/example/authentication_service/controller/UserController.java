package com.example.authentication_service.controller;

import com.example.authentication_service.DTO.ChangePasswordRequest;
import com.example.authentication_service.DTO.RegistrationRequestDto;
import com.example.authentication_service.model.User;
import com.example.authentication_service.service.RegistrationService;
import com.example.authentication_service.service.AuthenticationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private AuthenticationService authenticationService;

    private RegistrationService registrationService;



    @Autowired
    public UserController(AuthenticationService authenticationService, RegistrationService registrationService) {

        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
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
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody RegistrationRequestDto registrationRequest,
            BindingResult bindingResult
    ) {
        // 1. Валидация входных данных
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors()
                    .stream()
                    .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        // 2. Регистрация пользователя
        try {
            String result = registrationService.registerUser(registrationRequest);

            // 3. Обработка результатов
            if (result.equals("Успешно зарегестрирован")) {
                return ResponseEntity.status(HttpStatus.CREATED).body(
                        Collections.singletonMap("message", result)
                );
            } else {
                // Возвращаем сообщение об ошибке из user-service
                return ResponseEntity.badRequest().body(
                        Collections.singletonMap("error", result)
                );
            }

        } catch (ResponseStatusException e) {
            // 4. Обработка ResponseStatusException
            return ResponseEntity.status(e.getStatusCode()).body(
                    Collections.singletonMap("error", e.getReason())
            );
        } catch (Exception e) {
            // 5. Обработка непредвиденных ошибок
            return ResponseEntity.internalServerError().body(
                    Collections.singletonMap("error", "Внутренняя ошибка сервера")
            );
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


    @PutMapping("/{userId}/roles")
    public ResponseEntity<User> addRoleToUser(@PathVariable Long userId, @RequestBody String role) {
        User user = authenticationService.addRoleToUser(userId, role);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }


    @GetMapping("/roles/{userId}")
    public ResponseEntity<?> getUserRole(@PathVariable("id")Long id){
        try {
            Set<String> roles = authenticationService.getUserRole(id);
            return ResponseEntity.ok(roles);
        }catch(EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Пользователь по такому id не найден");
        }

    }

    @PostMapping("/send-verification-email")
    public ResponseEntity<String> sendVerificationEmail(@RequestParam("userId") Long userId) {
        try {
            authenticationService.sendVerificationEmail(userId);
            return ResponseEntity.ok("Письмо для подтверждения email отправлено.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при отправке письма: " + e.getMessage());
        }
    }

    // Другие методы контроллера (например, для аутентификации, обновления, удаления)
}