package com.example.kwizi.controller; // Замените на ваш пакет


import com.example.kwizi.DTO.request.AuthenticationRequest;
import com.example.kwizi.DTO.request.ChangePasswordRequest;
import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.DTO.response.AuthenticationResponse;
import com.example.kwizi.repository.RevokedTokenRepository;
import com.example.kwizi.security.JwtUtils;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.RegistrationService;
import com.example.kwizi.service.UserService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        logger.info("Запрос смены пароля для пользователя: {}", userDetails.getUsername());

        authenticationService.changePassword(userDetails.getUsername(), request);

        logger.info("Пароль успешно изменен для пользователя: {}", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Пароль успешно изменен",null));

    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequest registrationRequest) {

        logger.info("Запрос регистрации пользователя: {}", registrationRequest.getUsername());

        registrationService.registerUser(registrationRequest);

        logger.info("Пользователь успешно зарегистрирован: {}", registrationRequest.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Пользователь успешно зарегистрирован",null));

    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        String username = authenticationRequest.getUsername();
        logger.info("Запрос аутентификации пользователя: {}", username);

        logger.debug("Попытка аутентификации для пользователя: {}", username);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, authenticationRequest.getPassword()));


        logger.debug("Загрузка UserDetails для пользователя: {}", username);
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        logger.debug("Генерация JWT токена для пользователя: {}", username);
        final String jwt = jwtUtils.generateToken(userDetails.getUsername(),(userService.findByUsername(userDetails.getUsername()).get().getId()));

        logger.info("Успешная аутентификация пользователя: {}", username);
        return ResponseEntity.ok((new AuthenticationResponse(jwt)));

    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        logger.info("Запрос выхода из системы");

        String token = jwtUtils.extractToken(request);
        if (token == null) {
            logger.warn("Попытка выхода без токена");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Токен отсутствует",null));
        }
        authenticationService.logout(token);

        logger.info("Успешный выход пользователя");
        return ResponseEntity.ok(ApiResponse.success("Выход выполнен успешно", null));
    }

}



