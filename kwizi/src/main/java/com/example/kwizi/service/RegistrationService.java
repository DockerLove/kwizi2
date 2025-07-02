package com.example.kwizi.service;
import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.model.User;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final AuthenticationService authenticationService;

    public RegistrationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void registerUser(RegistrationRequest registrationRequest) {
        // 1. Check if username is already taken
        if (authenticationService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            throw new IllegalStateException("Пользователь с таким username уже есть.");
        }
        if(authenticationService.findByEmail(registrationRequest.getEmail()).isPresent()){
            throw new IllegalStateException("Пользователь с таким email уже есть.");
        }

        // 2. Create user in auth service
        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setBio(registrationRequest.getBio());
        user.setEmail(registrationRequest.getEmail());
        user.setFirstName(registrationRequest.getFirstName());
        user.setLastName(registrationRequest.getLastName());
        user.setPassword(registrationRequest.getPassword());
        authenticationService.registerUser(user);
        // 3. Create user in user-service
    }

    public void deleteUser(User user){
        authenticationService.delete(user);
    }
}
