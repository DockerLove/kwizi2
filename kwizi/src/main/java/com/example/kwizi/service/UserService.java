package com.example.kwizi.service;

import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.UserRepository;
import com.example.kwizi.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Autowired
    public UserService(UserRepository userRepository,JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public void verifyUserEmail(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        if (user.isEmail_verified()) {
            throw new IllegalArgumentException("Email уже подтвержден");
        }
        user.setEmail_verified(true);
        userRepository.save(user);

    }

    public void updateBio(Long id, String bio){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        user.setBio(bio);
        userRepository.save(user);
    }

    public void updateFirstName(Long id, String firstName){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        user.setFirstName(firstName);
        userRepository.save(user);
    }

    public void updateLastName(Long userId, String lastName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        user.setLastName(lastName);
        userRepository.save(user);
    }

    public String updateUsername(Long userId, String username) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Имя пользователя занято");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        user.setUsername(username);
        userRepository.save(user);

        return jwtUtils.generateToken(username);
    }


    public UserProfileResponse getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getBio(),
                user.getEmail()
        );
    }

    public boolean getEmailVerified(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        return user.isEmail_verified();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
