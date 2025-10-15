package com.example.kwizi.service;

import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.UserRepository;
import com.example.kwizi.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Autowired
    public UserService(UserRepository userRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public void verifyUserEmail(Long id) {
        executeWithLogging("верификация email", id, () -> {
            User user = findUserById(id);
            if (user.isEmail_verified()) {
                throw new IllegalArgumentException("Email уже подтвержден");
            }
            user.setEmail_verified(true);
        });
    }

    public String updateUsername(Long userId, String username) {
        return executeWithLogging("обновление username", userId, () -> {
            // Проверяем что новый username не занят
            validateUsernameNotExists(username);

            // Находим пользователя
            User user = findUserById(userId);

            // Обновляем username
            user.setUsername(username);
            userRepository.save(user);

            // Генерируем новый токен
            String token = jwtUtils.generateToken(username);
            logger.info("Сгенерирован новый токен для пользователя ID: {}", userId);

            return token;
        });
    }

    // Вспомогательный метод для проверки username
    private void validateUsernameNotExists(String username) {
        if (userRepository.existsByUsername(username)) {
            logger.warn("Попытка использования занятого username: {}", username);
            throw new IllegalArgumentException("Имя пользователя занято");
        }
    }

    public void updateBio(Long id, String bio) {
        executeWithLogging("обновление bio", id, () -> {
            User user = findUserById(id);
            user.setBio(bio);
            userRepository.save(user);
        });
    }

    public void updateFirstName(Long id, String firstName) {
        executeWithLogging("обновление имени", id, () -> {
            User user = findUserById(id);
            user.setFirstName(firstName);
            userRepository.save(user);
        });
    }

    public void updateLastName(Long userId, String lastName) {
        executeWithLogging("обновление фамилии", userId, () -> {
            User user = findUserById(userId);
            user.setLastName(lastName);
            userRepository.save(user);
        });
    }

    public UserProfileResponse getUserProfile(Long id) {
        return executeWithLogging("получение профиля", id, () -> {
            User user = findUserById(id);
            UserProfileResponse userProfileResponse = new UserProfileResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getUsername(),
                    user.getBio(),
                    user.getEmail()
            );
            return userProfileResponse;
        });
    }

    public boolean getEmailVerified(Long id) {
        return executeWithLogging("проверка верификации email", id, () -> {
            User user = findUserById(id);
            return user.isEmail_verified();
        });
    }

    public Optional<User> findByUsername(String username) {
        logger.debug("Поиск пользователя по username: {}", username);
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            logger.debug("Пользователь с username {} найден", username);
        } else {
            logger.debug("Пользователь с username {} не найден", username);
        }
        return user;
    }

    public Optional<User> findById(Long id) {
        logger.debug("Поиск пользователя по ID: {}", id);
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            logger.debug("Пользователь с ID {} найден", id);
        } else {
            logger.debug("Пользователь с ID {} не найден", id);
        }
        return user;
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден, ID: {}", id);
                    return new UserNotFoundException("Пользователь не найден");
                });
    }

    private void executeWithLogging(String operation, Long userId, Runnable action) {
        logger.info("Начало {} для пользователя ID: {}", operation, userId);
        try {
            action.run();
            logger.info("Успешное завершение {} для пользователя ID: {}", operation, userId);
        } catch (Exception e) {
            logger.error("Ошибка при {} для пользователя ID: {}", operation, userId, e);
            throw e;
        }
    }

    private <T> T executeWithLogging(String operation, Long userId, Supplier<T> action) {
        logger.info("Начало {} для пользователя ID: {}", operation, userId);
        try {
            T result = action.get();
            logger.info("Успешное завершение {} для пользователя ID: {}", operation, userId);
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при {} для пользователя ID: {}", operation, userId, e);
            throw e;
        }
    }
}