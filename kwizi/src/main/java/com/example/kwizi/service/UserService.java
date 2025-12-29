package com.example.kwizi.service;

import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.exception.BusinessLogicException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.UserRepository;
import com.example.kwizi.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.function.Supplier;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final FileStorageService fileStorageService;

    @Autowired
    public UserService(UserRepository userRepository, JwtUtils jwtUtils,FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.fileStorageService = fileStorageService;
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
    @Transactional
    public String updateUsername(Long userId, String username) {
        return executeWithLogging("обновление username", userId, () -> {
            // Проверяем что новый username не занят
            validateUsernameNotExists(username);

            // Находим пользователя
            User user = findUserById(userId);

            // Обновляем username
            user.setUsername(username);

            // Генерируем новый токен
            String token = jwtUtils.generateToken(username,user.getId());
            logger.info("Сгенерирован новый токен для пользователя ID: {}", userId);

            return token;
        });
    }
    @Transactional
    public void updateBio(Long id, String bio) {
        executeWithLogging("обновление bio", id, () -> {
            User user = findUserById(id);
            user.setBio(bio);
        });
    }
    @Transactional
    public void updateFirstName(Long id, String firstName) {
        executeWithLogging("обновление имени", id, () -> {
            User user = findUserById(id);
            user.setFirstName(firstName);
        });
    }

    @Transactional
    public void updateLastName(Long userId, String lastName) {
        executeWithLogging("обновление фамилии", userId, () -> {
            User user = findUserById(userId);
            user.setLastName(lastName);
        });
    }

    @Transactional
    public void updateEmail(Long userId,String email){
        executeWithLogging("обновление фамилии", userId, () -> {
            User user = findUserById(userId);
            user.setEmail(email);
            user.setEmail_verified(false);
        });
    }

    @Transactional
    public void updateUserAvatar(MultipartFile file, Long requesterId) {
        logger.info("Обновление аватара для пользователя с ID: {}", requesterId);

        User user = findUserById(requesterId);

        if (file.isEmpty()) {
            throw new BusinessLogicException("Файл не может быть пустым");
        }

        String avatarUrl = fileStorageService.saveUserAvatar(file, requesterId);

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        logger.info("Аватар успешно обновлен для пользователя с ID: {}", requesterId);

    }

    @Transactional
    public UserProfileResponse findUsername(String username,Long requestId){
        logger.info("Поиск пользователя по username: '{}' от пользователя ID: {}",
                username, requestId);

        User user = findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        if (user.getId().equals(requestId)) {
            throw new IllegalArgumentException("Нельзя искать самого себя");
        }

        UserProfileResponse userProfileResponse = new UserProfileResponse(
                user.getId(), user.getFirstName(), user.getLastName(),
                user.getUsername(), user.getBio(), user.getEmail());

        logger.info("Пользователь {} найден для пользователя ID: {}",
                username, requestId);

        return userProfileResponse;
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
    private void validateUsernameNotExists(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Имя пользователя занято");
        }
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
                    throw new UserNotFoundException("Пользователь не найден");
                });
    }

    private void executeWithLogging(String operation, Long userId, Runnable action) {
        logger.info("Начало {} для пользователя ID: {}", operation, userId);
        try {
            action.run();
            logger.info("Успешное завершение {} для пользователя ID: {}", operation, userId);
        } catch (Exception e) {
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
            throw e;
        }
    }
}