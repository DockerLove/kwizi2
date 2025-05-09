package com.example.authentication_service.Feign;

import com.example.authentication_service.DTO.UserDto;
import com.example.authentication_service.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(url = "http://localhost:8081",name = "user-service") // Имя сервиса
public interface UserServiceFeignClient {

    @PostMapping("/api/users")
    ResponseEntity<?> createUser(@RequestBody UserDto userDto); // Замени User на DTO
}