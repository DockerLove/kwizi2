package com.example.kwizi.service;
import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final AuthenticationService authenticationService;

    @Autowired // Corrected: Use @Autowired for dependency injection
    public RegistrationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void registerUser(RegistrationRequest registrationRequest) {
        logger.info("Запрос на регистрацию пользователя с username: {}", registrationRequest.getUsername());

        if (authenticationService.existsByUsername(registrationRequest.getUsername())) {
            logger.warn("Попытка регистрации с существующим username: {}", registrationRequest.getUsername());
            throw new IllegalStateException("Пользователь с таким username уже есть.");
        }
        if (authenticationService.findByEmail(registrationRequest.getEmail()).isPresent()) {
            logger.warn("Попытка регистрации с существующим email: {}", registrationRequest.getEmail());
            throw new IllegalStateException("Пользователь с таким email уже есть.");
        }

        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setBio(registrationRequest.getBio());
        user.setEmail(registrationRequest.getEmail());
        user.setFirstName(registrationRequest.getFirstName());
        user.setLastName(registrationRequest.getLastName());
        user.setPassword(registrationRequest.getPassword());

        authenticationService.registerUser(user);
        logger.info("Пользователь с username {} успешно зарегистрирован", registrationRequest.getUsername());
    }
}
