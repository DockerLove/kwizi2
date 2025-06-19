package com.example.kwizi.service;

import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void verifyUserEmail(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setEmail_verified(true);
        userRepository.save(user);

    }

    public void updateBio(Long id, String bio){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setBio(bio);
        userRepository.save(user);
    }

    public void updateFirstName(Long id, String firstName){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setFirstName(firstName);
        userRepository.save(user);
    }

    public void updateLastName(Long id, String lastName){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setLastName(lastName);
        userRepository.save(user);
    }

    public void updateUsername(Long id, String username){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if(userRepository.existsByUsername(username)){
            throw new IllegalArgumentException("Пользователь с таким ником уже используется");
        }
        user.setUsername(username);
        userRepository.save(user);
    }


    public UserProfileResponse getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getBio(),
                user.getEmail(),
                user.getPhoneNumber()
        );
    }

    public boolean getEmailVerified(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return user.isEmail_verified();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
