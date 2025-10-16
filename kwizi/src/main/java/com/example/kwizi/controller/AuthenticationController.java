package com.example.kwizi.controller; // Замените на ваш пакет

import com.example.kwizi.DTO.request.ChangePasswordRequest;
import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.exception.EmailAlreadyExistsException;
import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.exception.UsernameAlreadyExistsException;
import com.example.kwizi.model.RevokedToken;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.RevokedTokenRepository;
import com.example.kwizi.security.JwtUtils;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.RegistrationService;
import com.example.kwizi.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private AuthenticationManager authenticationManager;
    private AuthenticationService authenticationService;
    private UserDetailsService userDetailsService;

    private RegistrationService registrationService;
    private UserService userService;
    private JwtUtils jwtUtils;
    private final RevokedTokenRepository revokedTokenRepo;


    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                                    JwtUtils jwtUtils, RegistrationService registrationService,
                                    RevokedTokenRepository revokedTokenRepo,AuthenticationService authenticationService,UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.registrationService = registrationService;
        this.revokedTokenRepo = revokedTokenRepo;
        this.authenticationService = authenticationService;
        this.userService = userService;
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
            @Valid @RequestBody RegistrationRequest registrationRequest,
            BindingResult bindingResult
    ) {
        logger.info("Received registration request for username: {}", registrationRequest.getUsername()); // Логируем начало обработки запроса

        // 1. Валидация входных данных
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors()
                    .stream()
                    .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                    .collect(Collectors.toList());
            logger.warn("Registration validation failed for username {}. Errors: {}", registrationRequest.getUsername(), errors); // Логируем ошибку валидации
            return ResponseEntity.badRequest().body(errors);
        }

        // 2. Регистрация пользователя
        try {
            registrationService.registerUser(registrationRequest);
            logger.info("User registered successfully: {}", registrationRequest.getUsername()); // Логируем успешную регистрацию
            return new ResponseEntity<>(HttpStatus.CREATED); // Успешное создание пользователя
        } catch (UsernameAlreadyExistsException e) {
            logger.warn("Ошибка регистрации: данный username уже занят {}", registrationRequest.getUsername()); // Логируем ошибку, username занят
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT); // 409 Conflict
        } catch (EmailAlreadyExistsException e) {
            logger.warn("Ошибка регистрации: данный email уже занят {}", registrationRequest.getEmail()); // Логируем ошибку, username занят
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT); // 409 Conflict
        }  catch (IllegalStateException e) {
            logger.warn("Registration failed: username already exists: {}", registrationRequest.getUsername()); // Логируем ошибку, username занят
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT); // 409 Conflict
        }  catch (Exception e) {
            logger.error("Error during user registration for username {}", registrationRequest.getUsername(), e); // Логируем другие ошибки
            return new ResponseEntity<>("Произошла ошибка при регистрации пользователя", HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String username = null;
        try { //  Добавил try-catch чтобы  логировать в случае если что-то пойдет не так
            // 1. Извлекаем токен
            String token = jwtUtils.extractToken(request);
            if (token == null) {
                throw new JwtAuthenticationException("Токен отсутствует");
            }

            // 2. Проверяем и добавляем в чёрный список
            String jti = jwtUtils.extractJti(token);
            Date expiresAt = jwtUtils.extractExpiration(token);
            username = jwtUtils.getUsernameFromToken(token);
            Optional<User> user = userService.findByUsername(username);

            if (user.isEmpty()) {
                throw new JwtAuthenticationException("Пользователь не найден");
            }

            // Создаем RevokedToken с userId
            RevokedToken revokedToken = new RevokedToken(
                    jti,
                    user.get().getId(),  // Long userId
                    expiresAt,
                    username
            );

            revokedTokenRepo.save(revokedToken);
            logger.info("User {} successfully logged out", username);
        } catch (JwtAuthenticationException e) {
            logger.warn("Logout failed: {}", e.getMessage()); // Логируем ошибки, связанные с JWT
            if (username != null) { // если username был получен, логируем его для лучшей отладки
                logger.warn("Logout failed for user {}: {}", username, e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Error during logout", e); // Логируем другие ошибки
        }
        // 3. Возвращаем успешный ответ
        return ResponseEntity.noContent().build();
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