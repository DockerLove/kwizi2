package com.example.kwizi.service;


import com.example.kwizi.DTO.request.ChangePasswordRequest;
import com.example.kwizi.exception.AuthenticationService.EmailAlreadyVerifiedException;
import com.example.kwizi.exception.AuthenticationService.InvalidPasswordException;
import com.example.kwizi.exception.AuthenticationService.InvalidTokenException;
import com.example.kwizi.exception.AuthenticationService.TokenExpiredException;
import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.RevokedToken;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import com.example.kwizi.repository.RevokedTokenRepository;
import com.example.kwizi.security.JwtEmailVerify;
import com.example.kwizi.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEmailVerify jwtEmailVerify;
    private final EmailService emailService;
    private final JwtUtils jwtUtils;
    private UserService userService;
    private final RevokedTokenRepository revokedTokenRepo;


    @Autowired
    public AuthenticationService(AuthenticationRepository authenticationRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtEmailVerify jwtEmailVerify,
                                 EmailService emailService,JwtUtils jwtUtils,RevokedTokenRepository revokedTokenRepo,UserService userService) {
        this.authenticationRepository = authenticationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEmailVerify = jwtEmailVerify;
        this.emailService = emailService;
        this.revokedTokenRepo = revokedTokenRepo;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public void logout(String token) {
        logger.debug("Достаем данные из токена");

        String jti = jwtUtils.extractJti(token);
        Date expiresAt = jwtUtils.extractExpiration(token);
        String username = jwtUtils.getUsernameFromToken(token);

        logger.debug("Данные токена - JTI: {}, username: {}, expires: {}",
                jti, username, expiresAt);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> {
                    throw new UserNotFoundException("Пользователь не найден: " + username);
                });

        logger.debug("Создаем запись об отозванном токене");
        RevokedToken revokedToken = new RevokedToken(jti, user.getId(), expiresAt, username);
        revokedTokenRepo.save(revokedToken);

        logger.info("Токен отозван для пользователя: {}", username);
    }


    @Transactional
    public void sendVerificationEmail(Long userId) {
        logger.info("Отправка письма подтверждения для пользователя: {}", userId);

        User user = findUserById(userId);
        validateEmailNotVerified(user);

        String verificationToken = jwtEmailVerify.generateVerificationToken(user.getId());
        emailService.sendVerificationEmailAsync(user.getEmail(), verificationToken);

        logger.info("Письмо подтверждения отправлено пользователю: {}", userId);
    }

    @Transactional
    public void verifyEmail(String token) {
        logger.info("Подтверждение email по токену");

        validateTokenNotExpired(token);
        Long userId = extractUserIdFromToken(token);

        User user = findUserById(userId);
        user.setEmail_verified(true);

        logger.info("Email подтвержден для пользователя: {}", userId);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        logger.info("Смена пароля для пользователя: {}", username);

        User user = findUserByUsername(username);
        validateOldPassword(user, request.getOldPassword());
        validateNewPassword(user, request.getNewPassword());

        updateUserPassword(user, request.getNewPassword());
        logger.info("Пароль изменен для пользователя: {}", username);
    }

    @Transactional
    public User registerUser(User user) {
        logger.info("Регистрация пользователя: {}", user.getUsername());

        encodeUserPassword(user);
        User savedUser = authenticationRepository.save(user);

        logger.info("Пользователь зарегистрирован: {}", user.getUsername());
        return savedUser;
    }

    public boolean existsByUsername(String username) {
        logger.debug("Проверка существования пользователя: {}", username);
        return authenticationRepository.existsByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        logger.debug("Поиск пользователя по email: {}", email);
        return authenticationRepository.findByEmail(email);
    }


    private User findUserById(Long userId) {
        return authenticationRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userId));
    }

    private User findUserByUsername(String username) {
        return authenticationRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
    }

    private void validateEmailNotVerified(User user) {
        if (user.isEmail_verified()) {
            throw new EmailAlreadyVerifiedException("Email уже подтвержден для пользователя: " + user.getUsername());
        }
    }

    private void validateTokenNotExpired(String token) {
        if (jwtEmailVerify.isTokenExpired(token)) {
            throw new TokenExpiredException("Срок действия токена истек");
        }
    }

    private Long extractUserIdFromToken(String token) {
        try {
            String userIdString = jwtEmailVerify.getUserIdFromToken(token);
            return Long.parseLong(userIdString);
        } catch (JwtAuthenticationException e) {
            throw new InvalidTokenException("Неверный токен: " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new InvalidTokenException("Неверный формат ID пользователя в токене");
        }
    }

    private void validateOldPassword(User user, String oldPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidPasswordException("Неверный старый пароль");
        }
    }

    private void updateUserPassword(User user, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
    }

    private void encodeUserPassword(User user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
    }

    private void validateNewPassword(User user, String newPassword) {
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Новый пароль не должен совпадать со старым");
        }
    }
}