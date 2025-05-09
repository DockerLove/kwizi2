package com.example.authentication_service.controller;

import com.example.authentication_service.DTO.RegistrationRequestDto;
import com.example.authentication_service.DTO.UserDto;
import com.example.authentication_service.exception.ErrorResponse;
import com.example.authentication_service.model.User;
import com.example.authentication_service.service.RegistrationService;
import com.example.authentication_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private UserService userService;

    private RegistrationService registrationService;

    private final String userServiceUrl = "http://user_service:8081";

    @Autowired
    public UserController(UserService userService, RegistrationService registrationService) {

        this.userService = userService;
        this.registrationService = registrationService;
    }


    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegistrationRequestDto registrationRequest) {
        try {
            String result = registrationService.registerUser(registrationRequest);

            if (result.equals("Username is already taken")) {
                return ResponseEntity.badRequest().body(result);
            } else if (result.equals("Error creating user in user-service")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            } else {
                return ResponseEntity.status(HttpStatus.CREATED).body(result);
            }

        } catch (Exception e) {
            // Log the error
            System.err.println("UserController error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during registration");
        }
    }


    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(){
        List<User> users = userService.findAll();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<User> addRoleToUser(@PathVariable Long userId, @RequestBody String role) {
        User user = userService.addRoleToUser(userId, role);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
    // Другие методы контроллера (например, для аутентификации, обновления, удаления)
}