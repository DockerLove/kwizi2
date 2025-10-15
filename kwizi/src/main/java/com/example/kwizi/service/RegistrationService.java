package com.example.kwizi.service;

import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.exception.EmailAlreadyExistsException;
import com.example.kwizi.exception.UsernameAlreadyExistsException;
import com.example.kwizi.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final AuthenticationService authenticationService;

    @Autowired
    public RegistrationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Transactional
    public void registerUser(RegistrationRequest registrationRequest) {
        logger.info("Начало регистрации пользователя с username: {}", registrationRequest.getUsername());

        validateRegistrationRequest(registrationRequest);

        User user = createUserFromRequest(registrationRequest);
        authenticationService.registerUser(user);

        logger.info("Пользователь с username {} успешно зарегистрирован", registrationRequest.getUsername());
    }

    private void validateRegistrationRequest(RegistrationRequest request) {
        if (authenticationService.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Пользователь с таким username уже существует");
        }

        if (authenticationService.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Пользователь с таким email уже существует");
        }
    }

    private User createUserFromRequest(RegistrationRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setBio(request.getBio());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(request.getPassword());
        return user;
    }
}