package com.example.authentication_service.Feign;

import com.example.authentication_service.DTO.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(url = "http://localhost:8081",name = "user-service", configuration = com.example.authentication_service.config.FeignConfig.class) // Имя сервиса
public interface UserServiceFeignClient {

    @PostMapping("/api/users")
    ResponseEntity<Map<String,String>> createUser(@RequestBody UserDto userDto); // Замени User на DTO

    @GetMapping("/api/users/{userId}/email")
    ResponseEntity<String> getUserEmail(@PathVariable("userId") Long userId);

    @PatchMapping("/api/users/{userId}/verify-email")
    ResponseEntity<String> verifyUserEmail(@PathVariable("userId") Long userId);
}