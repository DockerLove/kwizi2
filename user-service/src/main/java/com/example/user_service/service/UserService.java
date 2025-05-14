package com.example.user_service.service;
import com.example.user_service.DTO.UpdateUserDto;
import com.example.user_service.DTO.UserDto;
import com.example.user_service.entity.User;
import com.example.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; // Убедись, что этот импорт есть

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(UserDto userDto) {
        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("Email уже используется");
        }
        User user = new User();
        user.setUserId(userDto.getUserId());
        user.setEmail(userDto.getEmail());
        user.setLastName(userDto.getLastName());
        user.setFirstName(userDto.getFirstName());
        return userRepository.save(user);
    }

    public User updateUser(Long id, UpdateUserDto updateUserDto){
        User user = userRepository.findById(id).orElse(null); //находим по id, если нет то присваиваем null
        if (user != null){
            user.setEmail(updateUserDto.getEmail());
            user.setFirstName(updateUserDto.getFirstName());
            user.setLastName(updateUserDto.getLastName());
        }
        return userRepository.save(user);
    }

    public void verifyUserEmail(Long userId){
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден с ID: " + userId));
        user.setEmail_verified(true);
        userRepository.save(user);

    }


    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public String getUserEmail(Long userId){
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден с ID: " + userId));
        return user.getEmail();
    }
}