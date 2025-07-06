package com.example.kwizi.service;
import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.UserRepository;
import com.example.kwizi.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Подключаем Mockito к JUnit 5
public class UserServiceTest {

    @Mock
    private UserRepository userRepository; // Мок репозитория

    @Mock
    private JwtUtils jwtUtils; // Мок утилиты для JWT (хотя здесь он не нужен для getUserProfile)

    @InjectMocks
    private UserService userService; // Тестируемый сервис (внедряем моки автоматически)
    private User createTestUser(Long id){
        User mockUser = new User();
        mockUser.setId(id);
        mockUser.setEmail_verified(false);
        mockUser.setUsername("ivan_ivanov");
        mockUser.setEmail("ivan@example.com");
        mockUser.setPassword("encodedPassword");
        mockUser.setFirstName("Иван");
        mockUser.setLastName("Петров");
        mockUser.setBio("bio");
        mockUser.setCreatedAt(OffsetDateTime.now());
        return mockUser;
    }
    // Тест для getUserProfile
    @Test
    void getUserProfile_ShouldReturnProfile_WhenUserExists() {
        // 1. Подготовка тестовых данных
        Long userId = 1L;
        User mockUser = createTestUser(userId);

        // 2. Настраиваем поведение мока
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // 3. Вызываем тестируемый метод
        UserProfileResponse response = userService.getUserProfile(userId);

        // 4. Проверяем результаты
        assertNotNull(response);
        assertEquals("Иван", response.getFirstName());
        assertEquals("Петров", response.getLastName());
        assertEquals("ivan_ivanov", response.getUsername());
        assertEquals("bio", response.getBio());
        assertEquals("ivan@example.com", response.getEmail());

        // 5. Проверяем, что findById вызвался ровно 1 раз
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserProfile_ShouldThrowException_WhenUserNotFound() {
        // 1. Настраиваем мок так, чтобы он возвращал пустой Optional
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // 2. Проверяем, что метод выбрасывает исключение
        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserProfile(1L);
        });

        // 3. Проверяем вызов findById
        verify(userRepository, times(1)).findById(anyLong());
    }

    @Test
    void findById_UserExists_ReturnsUser() {
        Long userId = 1L;
        User mockUser = createTestUser(userId);

        // 2. Задаем поведение мока
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // 3. Вызов метода
        Optional<User> result = userService.findById(userId);

        // 4. Проверки
        assertTrue(result.isPresent(), "Пользователь должен быть найден");
        assertEquals(userId, result.get().getId(), "ID пользователя должен совпадать");
        assertEquals("ivan_ivanov", result.get().getUsername(), "Username должен совпадать");
        verify(userRepository, times(1)).findById(userId); // Проверяем, что метод вызван ровно 1 раз
    }

    @Test
    void findById_WhenUserNotExists_ReturnsEmpty() {
        Long id = 111L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        Optional<User> user = userService.findById(id);
        assertFalse(user.isPresent(), "Пользователь не найден!");
        verify(userRepository,times(1)).findById(id);
    }
    @Test
    void findByUsername_WhenUserExists_ReturnUser(){
        Long id = 1L;
        User mockUser = createTestUser(id);

        when(userRepository.findByUsername(mockUser.getUsername())).thenReturn(Optional.of(mockUser));

        Optional<User> user = userService.findByUsername(mockUser.getUsername());

        assertTrue(user.isPresent(), "Пользователь должен быть найден");
        assertEquals(mockUser.getUsername(),user.get().getUsername(), "Username должен совпадать");
        verify(userRepository,times(1)).findByUsername(mockUser.getUsername());
    }
    @Test
    void findByUsername_WhenUserNotExists_ReturnEmpty(){
        String username = "Docker";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        Optional<User> user = userService.findByUsername(username);
        assertFalse(user.isPresent(), "Пользователь не должен быть найден");
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void getEmailVerified_WhenUserExistsAndEmailVerified_ReturnTrue(){
        Long userId = 1L;
        User mockUser = createTestUser(userId);
        mockUser.setEmail_verified(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // 3. Вызов метода и проверка
        assertTrue(userService.getEmailVerified(userId));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getEmailVerified_WhenUserExistsAndEmailNotVerified_ReturnFalse(){
        Long userId = 1L;
        User mockUser = createTestUser(userId);
        mockUser.setEmail_verified(false);


        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        assertFalse(userService.getEmailVerified(userId),"Должен вернуть false");
        verify(userRepository,times(1)).findById(userId);
    }

    @Test
    void getEmailVerified_WhenUserNotExists_ThrowsException(){
        Long id = 12L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.getEmailVerified(id));

        assertEquals("Пользователь не найден",exception.getMessage());
        verify(userRepository,times(1)).findById(id);
    }

    @Test
    void updateLastName_WhenUserExists_UpdatesLastName(){
        Long id = 1L;
        String newLastName = "Сидоров";
        User mockUser = createTestUser(id);

        when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

        userService.updateLastName(id,newLastName);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());


        User user = userCaptor.getValue();
        assertEquals(id,user.getId(),"Id должен совпадать");
        assertEquals(newLastName,user.getLastName(),"lastName должен совпасть");
    }

    @Test
    void updateLastName_WhenUserNotExists_ThrowsException(){
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.updateLastName(id, "Сидоров"));

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateBio_WhenUserExists_UpdatesBioAndSavesUser() {
        // 1. Подготовка данных
        Long userId = 1L;
        String newBio = "Сидоров";
        User existingUser = createTestUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // 2. Вызов метода
        userService.updateBio(userId, newBio);

        // 3. Проверка, что bio изменилось
        assertEquals(newBio, existingUser.getBio());

        // 4. Проверка вызова save с правильным пользователем
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(userId, savedUser.getId());
        assertEquals(newBio, savedUser.getBio());
    }

    @Test
    void updateBio_WhenUserNotExists_ThrowsException() {
        Long nonExistentUserId = 999L;
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.updateBio(nonExistentUserId, "Любая биография"));

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateFirstName_WhenUserExists_UpdatesNameAndSavesUser() {
        // 1. Подготовка данных
        Long userId = 1L;
        String newName = "НовоеИмя";
        User existingUser = createTestUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // 2. Вызов метода
        userService.updateFirstName(userId, newName);

        // 3. Проверка, что имя изменилось
        assertEquals(newName, existingUser.getFirstName());

        // 4. Проверка вызова save
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(userId, savedUser.getId());
        assertEquals(newName, savedUser.getFirstName());
    }

    @Test
    void updateFirstName_WhenUserNotExists_ThrowsException() {
        Long nonExistentUserId = 999L;
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.updateFirstName(nonExistentUserId, "ЛюбоеИмя"));

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUsername_WhenUsernameAvailable_UpdatesAndReturnsToken() {
        // 1. Подготовка данных
        Long userId = 1L;
        String newUsername = "newUser";
        String expectedToken = "new.jwt.token";

        User user = createTestUser(userId);


        // 2. Настройка моков
        when(userRepository.existsByUsername(newUsername)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(newUsername)).thenReturn(expectedToken);

        // 3. Вызов метода
        String result = userService.updateUsername(userId, newUsername);

        // 4. Проверки
        // Проверяем обновление username
        assertEquals(newUsername, user.getUsername());

        // Проверяем вызов save
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(newUsername, userCaptor.getValue().getUsername());

        // Проверяем возвращенный токен
        assertEquals(expectedToken, result);

        // Проверяем вызовы зависимостей
        verify(userRepository).existsByUsername(newUsername);
        verify(userRepository).findById(userId);
        verify(jwtUtils).generateToken(newUsername);
    }

    @Test
    void updateUsername_WhenUsernameTaken_ThrowsException() {
        Long userId = 1L;
        String takenUsername = "existingUser";

        when(userRepository.existsByUsername(takenUsername)).thenReturn(true);

        // Проверяем исключение
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUsername(userId, takenUsername)
        );

        assertEquals("Имя пользователя занято", exception.getMessage());

        // Проверяем, что остальные методы не вызывались
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtUtils);
    }
    @Test
    void updateUsername_WhenUserNotFound_ThrowsException(){
        Long userId = 1l;
        String newUsername = "newUsername";

        when(userRepository.existsByUsername(newUsername)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.updateUsername(userId, newUsername));

        assertEquals("Пользователь не найден", exception.getMessage());

        verify(userRepository,never()).save(any());
        verifyNoInteractions(jwtUtils);
    }


    @Test
    @Transactional
    void verifyUserEmail_WhenUserExistsAnsEmailNotVerified_VerifiedUserEmail(){
        Long id = 2L;
        User user = createTestUser(id);
        user.setEmail_verified(false);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.verifyUserEmail(id));

        assertTrue(user.isEmail_verified());
        verify(userRepository,times(1)).save(user);
    }

    @Test
    @Transactional
    void verifyUserEmail_WhenEmailAlreadyVerified_ThrowsException() {
        Long userId = 2L;
        User verifiedUser = createTestUser(userId);
        verifiedUser.setEmail_verified(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(verifiedUser));

        // Проверяем, что выбрасывается исключение
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.verifyUserEmail(userId)
        );

        assertEquals("Email уже подтвержден", exception.getMessage());
        verify(userRepository, never()).save(any()); // Проверяем, что save не вызывался
    }

    @Test
    @Transactional
    void verifyUserEmail_WhenUserNotExists_ThrowsException() {
        Long nonExistentUserId = 999L;
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Проверяем, что выбрасывается исключение
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.verifyUserEmail(nonExistentUserId)
        );

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userRepository, never()).save(any()); // Проверяем, что save не вызывался
    }
}