package com.example.kwizi.service;


import com.example.kwizi.DTO.request.ChangePasswordRequest;
import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import com.example.kwizi.security.JwtEmailVerify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEmailVerify jwtEmailVerify;
    private final EmailService emailService;

    @Autowired
    public AuthenticationService(AuthenticationRepository authenticationRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtEmailVerify jwtEmailVerify,
                                 EmailService emailService) {
        this.authenticationRepository = authenticationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEmailVerify = jwtEmailVerify;
        this.emailService = emailService;
    }

    @Transactional
    public void sendVerificationEmail(Long id) {
        logger.info("Запрос на отправку письма для подтверждения email пользователю с ID: {}", id);
        try {
            User user = authenticationRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

            if (user.isEmail_verified()) {
                logger.warn("Попытка отправить письмо подтверждения для уже подтвержденного email пользователя с ID: {}", id);
                throw new IllegalArgumentException("Email уже подтвержден");
            }

            String verificationToken = jwtEmailVerify.generateVerificationToken(user.getId());
            emailService.sendVerificationEmailAsync(user.getEmail(), verificationToken);
            logger.info("Письмо для подтверждения email отправлено пользователю с ID: {}", id);
        } catch (UserNotFoundException e) {
            logger.warn("Пользователь не найден при отправке письма для подтверждения email, ID: {}", id);
            throw e; // Re-throw to be handled by a global exception handler
        } catch (IllegalArgumentException e) {
            logger.warn("Email уже подтвержден для пользователя с ID: {}", id);
            throw e; // Re-throw to be handled by a global exception handler
        } catch (Exception e) {
            logger.error("Ошибка при отправке письма для подтверждения email пользователю с ID: {}", id, e);
            throw new IllegalStateException("Ошибка при отправке письма для подтверждения email: " + e.getMessage()); // Rethrow as IllegalStateException
        }
    }

    public void verifyEmail(String token) {
        logger.info("Запрос на подтверждение email с токеном: {}", token);
        try {
            if (jwtEmailVerify.isTokenExpired(token)) {
                logger.warn("Срок действия токена истек: {}", token);
                throw new IllegalArgumentException("Срок действия токена истек.");
            }

            String userIdString = jwtEmailVerify.getUserIdFromToken(token);
            Long userId = Long.parseLong(userIdString);

            verifyUserEmail(userId);
            logger.info("Email успешно подтвержден для токена: {} и пользователя с ID: {}", token, userId);

        } catch (JwtAuthenticationException e) {
            logger.warn("Неверный токен при подтверждении email: {}", e.getMessage());
            throw new IllegalArgumentException("Неверный токен: " + e.getMessage());
        } catch (NumberFormatException e) {
            logger.warn("Неверный формат ID пользователя в токене");
            throw new IllegalArgumentException("Неверный формат id пользователя в токене.");
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка при подтверждении email: {}", e.getMessage());
            throw e; // Re-throw to be handled by a global exception handler
        } catch (Exception e) {
            logger.error("Произошла ошибка при подтверждении email: {}", e.getMessage(), e);
            throw new IllegalStateException("Произошла ошибка: " + e.getMessage()); // Re-throw as IllegalStateException
        }
    }

    @Transactional
    public void verifyUserEmail(Long id) {
        logger.info("Подтверждение email для пользователя с ID: {}", id);
        try {
            User user = authenticationRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
            user.setEmail_verified(true);
            authenticationRepository.save(user);
            logger.info("Email успешно подтвержден для пользователя с ID: {}", id);
        } catch (UserNotFoundException e) {
            logger.warn("Пользователь не найден при подтверждении email, ID: {}", id);
            throw e; // Re-throw to be handled by a global exception handler
        } catch (Exception e) {
            logger.error("Ошибка при подтверждении email для пользователя с ID: {}", id, e);
            throw new IllegalStateException("Произошла ошибка при подтверждении email: " + e.getMessage()); // Rethrow as IllegalStateException
        }
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        logger.info("Запрос на изменение пароля для пользователя: {}", username);
        try {
            User user = authenticationRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                logger.warn("Неверный старый пароль для пользователя: {}", username);
                throw new BadCredentialsException("Invalid old password");
            }

            String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
            user.setPassword(encodedNewPassword);
            authenticationRepository.save(user);
            logger.info("Пароль успешно изменен для пользователя: {}", username);
        } catch (UsernameNotFoundException e) {
            logger.warn("Пользователь не найден при смене пароля: {}", username);
            throw e;  // Re-throw to be handled by a global exception handler
        } catch (BadCredentialsException e) {
            logger.warn("Неверный старый пароль при смене пароля для пользователя: {}", username);
            throw e;  // Re-throw to be handled by a global exception handler
        } catch (Exception e) {
            logger.error("Ошибка при изменении пароля для пользователя {}: {}", username, e.getMessage(), e);
            throw new IllegalStateException("Произошла ошибка при смене пароля: " + e.getMessage()); // Rethrow as IllegalStateException
        }
    }

    @Transactional
    public User registerUser(User user) {
        logger.info("Регистрация нового пользователя с username: {}", user.getUsername());
        try {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
            User savedUser = authenticationRepository.save(user);
            logger.info("Пользователь {} успешно зарегистрирован", user.getUsername());
            return savedUser;
        } catch (Exception e) {
            logger.error("Ошибка при регистрации пользователя: {}", user.getUsername(), e);
            throw new IllegalStateException("Произошла ошибка при регистрации пользователя: " + e.getMessage()); // Re-throw as IllegalStateException
        }
    }

    public void delete(User user) {
        logger.info("Удаление пользователя с username: {}", user.getUsername());
        try {
            authenticationRepository.delete(user);
            logger.info("Пользователь {} успешно удален", user.getUsername());
        } catch (Exception e) {
            logger.error("Ошибка при удалении пользователя {}", user.getUsername(), e);
            throw new IllegalStateException("Произошла ошибка при удалении пользователя: " + e.getMessage()); // Rethrow as IllegalStateException
        }
    }

    public Optional<User> findByUsername(String username) {
        logger.debug("Поиск пользователя по username: {}", username);
        try {
            Optional<User> user = authenticationRepository.findByUsername(username);
            if (user.isPresent()) {
                logger.debug("Пользователь {} найден", username);
            } else {
                logger.debug("Пользователь {} не найден", username);
            }
            return user;
        } catch (Exception e) {
            logger.error("Ошибка при поиске пользователя по username {}: {}", username, e.getMessage(), e);
            throw new IllegalStateException("Произошла ошибка при поиске пользователя: " + e.getMessage()); // Rethrow as IllegalStateException
        }
    }

    public boolean existsByUsername(String username) {
        logger.debug("Проверка существования пользователя с username: {}", username);
        try {
            boolean exists = authenticationRepository.existsByUsername(username);
            logger.debug("Пользователь с username {} существует: {}", username, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Ошибка при проверке существования пользователя с username {}: {}", username, e.getMessage(), e);
            throw new IllegalStateException("Произошла ошибка при проверке существования пользователя: " + e.getMessage()); // Rethrow as IllegalStateException
        }
    }

    public Optional<User> findByEmail(String email) {
        logger.debug("Поиск пользователя по email: {}", email);
        try {
            Optional<User> user = authenticationRepository.findByEmail(email);
            if (user.isPresent()) {
                logger.debug("Пользователь с email {} найден", email);
            } else {
                logger.debug("Пользователь с email {} не найден", email);
            }
            return user;
        } catch (Exception e) {
            logger.error("Ошибка при поиске пользователя по email {}: {}", email, e.getMessage(), e);
            throw new IllegalStateException("Произошла ошибка при поиске пользователя: " + e.getMessage()); // Rethrow as IllegalStateException
        }
    }
}