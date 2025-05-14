package com.example.authentication_service.service;
import com.example.authentication_service.DTO.RegistrationRequestDto;
import com.example.authentication_service.DTO.UserDto;
import com.example.authentication_service.Feign.UserServiceFeignClient;
import com.example.authentication_service.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class RegistrationService {

    private final AuthenticationService authenticationService;
    private final UserServiceFeignClient userServiceFeignClient;

    public RegistrationService(AuthenticationService authenticationService, UserServiceFeignClient userServiceFeignClient) {
        this.authenticationService = authenticationService;
        this.userServiceFeignClient = userServiceFeignClient;
    }

    public String registerUser(RegistrationRequestDto registrationRequest) {
        // 1. Check if username is already taken
        if (authenticationService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            return "Имя пользователя занято";
        }
        // 2. Create user in auth service
        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(registrationRequest.getPassword());
        authenticationService.registerUser(user);
        // 3. Create user in user-service
        UserDto userDto = new UserDto();
        userDto.setUserId(user.getId());
        userDto.setEmail(registrationRequest.getEmail());
        userDto.setFirstName(registrationRequest.getFirstName());
        userDto.setLastName(registrationRequest.getLastName());
        try {
            ResponseEntity<Map<String, String>> response = userServiceFeignClient.createUser(userDto);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                return "Успешно зарегестрирован";
            } else {
                authenticationService.delete(user);
                return response.getBody().get("error");
            }

        } catch (ResponseStatusException e) { // Перехватываем ResponseStatusException
            authenticationService.delete(user);
            return e.getReason(); // Получаем сообщение об ошибке
        } catch (Exception e) {
            authenticationService.delete(user);
            return "Service unavailable";
        }
    }

    public void deleteUser(User user){
        authenticationService.delete(user);
    }
}
