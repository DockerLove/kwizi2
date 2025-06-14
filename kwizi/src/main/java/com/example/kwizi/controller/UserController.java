package com.example.kwizi.controller;

import com.example.kwizi.DTO.ChangePasswordRequest;
import com.example.kwizi.DTO.RegistrationRequestDto;
import com.example.kwizi.model.User;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.RegistrationService;
import com.example.kwizi.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    private UserService userService;

    private RegistrationService registrationService;



    @Autowired
    public UserController(AuthenticationService authenticationService, UserService userService,RegistrationService registrationService) {
        this.userService = userService;
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
            registrationService.registerUser(registrationRequest);
            return new ResponseEntity<>(HttpStatus.CREATED); // Успешное создание пользователя
        } catch (IllegalStateException e) {
            // Обработка исключения, если username уже занят
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT); // 409 Conflict
        } catch (Exception e) {
            // Обработка других исключений (логирование, и т.д.)
            return new ResponseEntity<>("Произошла ошибка при регистрации пользователя", HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error
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

    @PatchMapping("/{id}/verify-email")
    public ResponseEntity<?> verifyUserEmail(@PathVariable("id") Long id){
        try{
            userService.verifyUserEmail(id);
            return ResponseEntity.ok(("Email успешно подтвержден"));
        }catch(IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }



    @PostMapping("/send-verification-email/{id}")
    public ResponseEntity<String> sendVerificationEmail(@PathVariable("id") Long id) {
        try {
            authenticationService.sendVerificationEmail(id);
            return ResponseEntity.ok("Письмо для подтверждения email отправлено.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при отправке письма: " + e.getMessage());
        }
    }

    // Другие методы контроллера (например, для аутентификации, обновления, удаления)
}