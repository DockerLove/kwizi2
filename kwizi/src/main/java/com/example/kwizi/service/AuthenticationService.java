package com.example.kwizi.service;


import com.example.kwizi.DTO.request.ChangePasswordRequest;
import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import com.example.kwizi.security.JwtEmailVerify;
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
    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;

    private JwtEmailVerify jwtEmailVerify;

    private EmailService emailService;


    @Autowired
    public AuthenticationService(AuthenticationRepository authenticationRepository, PasswordEncoder passwordEncoder,JwtEmailVerify jwtEmailVerify, EmailService emailService
    ) {
        this.jwtEmailVerify = jwtEmailVerify;
        this.emailService = emailService;
        this.authenticationRepository = authenticationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void sendVerificationEmail(Long id) { // Используем id (из auth-service)
        // 1. Получить email пользователя из user-service
        User user = authenticationRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        if (user.isEmail_verified()) {
            throw new IllegalArgumentException("Email уже подтвержден");
        }
        // 3. Сгенерировать JWT токен
        String verificationToken = jwtEmailVerify.generateVerificationToken(user.getId());

        // 4. Отправить письмо с подтверждением email
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);
    }

    public void verifyEmail(String token) {
        try {

            if (jwtEmailVerify.isTokenExpired(token)) {
                throw new IllegalArgumentException("Срок действия токена истек.");
            }
            // 2. Извлечь id пользователя из токена
            String userIdString = jwtEmailVerify.getUserIdFromToken(token);
            Long userId = Long.parseLong(userIdString);

            // 4. Активировать пользователя (если он еще не активирован)
            verifyUserEmail(userId);
        } catch (JwtAuthenticationException e) {
            throw new IllegalArgumentException("Неверный токен: " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверный формат id пользователя в токене.");
        } catch (IllegalArgumentException e) {
            throw e; // Пробрасываем исключение выше
        } catch (Exception e) {
            throw new IllegalStateException("Произошла ошибка: " + e.getMessage());
        }
    }


    public void verifyUserEmail(Long id){
        User user = authenticationRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        user.setEmail_verified(true);
        authenticationRepository.save(user);

    }



    public void changePassword(String username, ChangePasswordRequest request){

        User user = authenticationRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 1. Проверка старого пароля
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid old password");
        }

        // 2. Хеширование нового пароля
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());

        // 3. Обновление пароля в базе данных
        user.setPassword(encodedNewPassword);
        authenticationRepository.save(user);
    }

    @Transactional
    public User registerUser(User user) {
        // Хэшируем пароль пользователя
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // Сохраняем пользователя в базе данных
        return authenticationRepository.save(user);
    }




    public void delete(User user){
        authenticationRepository.delete(user);
    }


    public Optional<User> findByUsername(String username) {
        return authenticationRepository.findByUsername(username);
    }


    public boolean existsByUsername(String username) {
        return authenticationRepository.existsByUsername(username);
    }


    public List<User> findAll(){
        return authenticationRepository.findAll();
    }
    // Другие методы сервиса (например, для аутентификации, обновления, удаления)

    public Optional<User> findByEmail(String email){
        return authenticationRepository.findByEmail(email);
    }
}
