package com.example.user_service.controller;
import com.example.user_service.DTO.UpdateUserDto;
import com.example.user_service.DTO.UserDto;
import com.example.user_service.entity.User;
import com.example.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserDto userDto) {
        try {
            User createdUser = userService.createUser(userDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(createdUser));
        } catch (IllegalArgumentException e) {
            // Возвращаем DTO с ошибкой вместо строки
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    // Метод для преобразования User в UserDto
    private UserDto convertToDto(User user) {
        UserDto userDto = new UserDto();
        // Если есть поле id
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setEmail(user.getEmail());
        return userDto;
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    //Обновление имени, фамилии и почты пользователя
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") Long id, @Valid @RequestBody UpdateUserDto updateUserDto, BindingResult bindingResult){
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors()
                    .stream()
                    .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors); // 400 Bad Request с ошибками валидации
        }

        User updateUser = userService.updateUser(id, updateUserDto);
        if(updateUser == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updateUser);

    }

    @PatchMapping("/{userId}/verify-email")
    public ResponseEntity<?> verifyUserEmail(@PathVariable("userId") Long userId){
        try{
            userService.verifyUserEmail(userId);
            return ResponseEntity.ok(("Email успешно подтвержден"));
        }catch(IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }


    @GetMapping("/{userId}/email")
    public ResponseEntity<String> getUserEmail(@PathVariable("userId")Long userId){
        try{
            String email = userService.getUserEmail(userId);
            return ResponseEntity.ok(email);
        }catch(IllegalArgumentException ex){
            System.out.println("Ошибка IllegalArgumentException: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}