package com.example.authentication_service.service;
import com.example.authentication_service.DTO.RegistrationRequestDto;
import com.example.authentication_service.DTO.UserDto;
import com.example.authentication_service.Feign.UserServiceFeignClient;
import com.example.authentication_service.model.User;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final UserService userService;
    private final UserServiceFeignClient userServiceFeignClient;

    public RegistrationService(UserService userService, UserServiceFeignClient userServiceFeignClient) {
        this.userService = userService;
        this.userServiceFeignClient = userServiceFeignClient;
    }

    public String registerUser(RegistrationRequestDto registrationRequest) {
        // 1. Check if username is already taken
        System.out.println("Метод findByUsername");
        if (userService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            return "Username is already taken";
        }
        System.out.println("создание User");
        // 2. Create user in auth service
        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(registrationRequest.getPassword());
        userService.registerUser(user);
        System.out.println("Сохранение user");
        System.out.println("создание UserDto");
        // 3. Create user in user-service
        UserDto userDto = new UserDto();
        userDto.setUserId(user.getId());
        userDto.setEmail(registrationRequest.getEmail());
        userDto.setFirstName(registrationRequest.getFirstName());
        userDto.setLastName(registrationRequest.getLastName());
        System.out.println("Создался UserDto");
        try {
            System.out.println("Сохранение UserDto");
            userServiceFeignClient.createUser(userDto);
            return "User registered successfully";

        } catch (Exception e) {
            // If user creation in user-service fails, delete user from auth service
            userService.delete(user);
            return "Error creating user in user-service";
        }
    }

    public void deleteUser(User user){
        userService.delete(user);
    }
}
