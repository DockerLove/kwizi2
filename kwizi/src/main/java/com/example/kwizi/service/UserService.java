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
        logger.info("Верификация email пользователя с ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден при верификации email, ID: {}", id);
                    return new UserNotFoundException("Пользователь не найден");
                });
        if (user.isEmail_verified()) {
            logger.warn("Попытка верификации уже подтвержденного email пользователя с ID: {}", id);
            throw new IllegalArgumentException("Email уже подтвержден");
        }
        user.setEmail_verified(true);
        userRepository.save(user);
        logger.info("Email успешно верифицирован для пользователя с ID: {}", id);
    }

    public String updateUsername(Long userId, String username) {
        logger.info("Обновление username пользователя с ID: {}, новый username: {}", userId, username);
        if (userRepository.existsByUsername(username)) {
            logger.warn("Попытка обновления username на уже существующий: {}", username);
            throw new IllegalArgumentException("Имя пользователя занято");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден при обновлении username, ID: {}", userId);
                    return new UserNotFoundException("Пользователь не найден");
                });

        user.setUsername(username);
        userRepository.save(user);

        String token = jwtUtils.generateToken(username);
        logger.info("Username пользователя с ID {} успешно обновлен, сгенерирован новый токен", userId);
        return token;
    }

    public void updateBio(Long id, String bio) {
        logger.info("Обновление bio пользователя с ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден при обновлении bio, ID: {}", id);
                    return new UserNotFoundException("Пользователь не найден");
                });
        user.setBio(bio);
        userRepository.save(user);
        logger.info("Bio пользователя с ID {} успешно обновлен", id);
    }

    public void updateFirstName(Long id, String firstName) {
        logger.info("Обновление firstName пользователя с ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден при обновлении firstName, ID: {}", id);
                    return new UserNotFoundException("Пользователь не найден");
                });
        user.setFirstName(firstName);
        userRepository.save(user);
        logger.info("FirstName пользователя с ID {} успешно обновлен", id);
    }

    public void updateLastName(Long userId, String lastName) {
        logger.info("Обновление lastName пользователя с ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден при обновлении lastName, ID: {}", userId);
                    return new UserNotFoundException("Пользователь не найден");
                });
        user.setLastName(lastName);
        userRepository.save(user);
        logger.info("LastName пользователя с ID {} успешно обновлен", userId);
    }


    public UserProfileResponse getUserProfile(Long id) {
        logger.info("Получение профиля пользователя с ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден при получении профиля, ID: {}", id);
                    return new UserNotFoundException("Пользователь не найден");
                });

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getBio(),
                user.getEmail()
        );
        logger.info("Профиль пользователя с ID {} успешно получен", id);
        return profile;
    }

    public boolean getEmailVerified(Long id) {
        logger.info("Получение статуса верификации email пользователя с ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден при получении статуса верификации email, ID: {}", id);
                    return new UserNotFoundException("Пользователь не найден");
                });
        boolean isVerified = user.isEmail_verified();
        logger.info("Статус верификации email пользователя с ID {}: {}", id, isVerified);
        return isVerified;
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
}