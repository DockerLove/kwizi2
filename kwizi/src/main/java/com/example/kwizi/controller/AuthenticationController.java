package com.example.kwizi.controller; // Замените на ваш пакет

import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.security.JwtUtils;
import com.example.kwizi.service.RegistrationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private AuthenticationManager authenticationManager;
    private UserDetailsService userDetailsService;

    private RegistrationService registrationService;
    private JwtUtils jwtUtils;

    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtUtils jwtUtils, RegistrationService registrationService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody RegistrationRequest registrationRequest,
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest authenticationRequest) {
        logger.info("Received authentication request for user: {}", authenticationRequest.getUsername());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            logger.warn("Incorrect username or password for user: {}", authenticationRequest.getUsername());
            return ResponseEntity.status(401).body("Incorrect username or password"); // Unauthorized
        } catch (Exception e) {
            logger.error("An error occurred during authentication for user {}: {}", authenticationRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(500).body("Internal server error"); // Internal Server Error
        }

        final UserDetails userDetails = userDetailsService
                .loadUserByUsername(authenticationRequest.getUsername());

        final String jwt = jwtUtils.generateToken(userDetails.getUsername());

        logger.info("Successfully authenticated user: {}", authenticationRequest.getUsername());
        return ResponseEntity.ok(new AuthenticationResponse(jwt));

        /*Этот метод обрабатывает POST-запросы на /api/users/authenticate.
Он принимает имя пользователя и пароль из тела запроса (AuthenticationRequest).
Он использует AuthenticationManager для аутентификации пользователя.
Если аутентификация прошла успешно, он вызывает jwtUtils.generateToken(userDetails) для генерации JWT.
Он возвращает JWT клиенту в теле ответа (AuthenticationResponse).*/
    }


    // Классы для представления запроса и ответа (вложенные классы для простоты)
}

class AuthenticationRequest {
    private String username;
    private String password;

    public AuthenticationRequest() {
    }

    public AuthenticationRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}


class AuthenticationResponse {
    private String jwt;

    public AuthenticationResponse() {
    }

    public AuthenticationResponse(String jwt) {
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}