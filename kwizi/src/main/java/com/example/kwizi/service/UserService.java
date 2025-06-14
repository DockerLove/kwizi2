package com.example.kwizi.service;

import com.example.kwizi.model.User;
import com.example.kwizi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void verifyUserEmail(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден с ID: " + id));
        user.setEmail_verified(true);
        userRepository.save(user);

    }
}
