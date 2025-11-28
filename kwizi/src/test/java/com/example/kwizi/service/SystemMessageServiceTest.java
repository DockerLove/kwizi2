package com.example.kwizi.service;

import com.example.kwizi.enums.MessageSystemType;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SystemMessageService systemMessageService;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    private final String USERNAME = "testuser";
    private final String ANOTHER_USERNAME = "anotheruser";
    private final String ADMIN_USERNAME = "adminuser";

    // ===== ТЕСТЫ ДЛЯ ОСНОВНЫХ МЕТОДОВ =====

    @Test
    void createUserAddedMessage_WithValidData_ShouldCreateCorrectMessage() {
        // Arrange
        Chat chat = createTestChat(1L, "Test Chat");
        User sender = createTestUser(1L, ADMIN_USERNAME);

        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));

        // Act
        systemMessageService.createUserAddedMessage(chat, USERNAME, ADMIN_USERNAME);

        // Assert
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getMessageType()).isEqualTo(MessageSystemType.USER_ADDED);
        assertThat(savedMessage.getText()).isEqualTo("Пользователь testuser добавлен в чат пользователем adminuser");
        assertThat(savedMessage.getChat()).isEqualTo(chat);
        assertThat(savedMessage.getSender()).isEqualTo(sender);
        assertThat(savedMessage.getCreatedAt()).isNotNull();
    }

    @Test
    void createUserRemovedMessage_WithValidData_ShouldCreateCorrectMessage() {
        // Arrange
        Chat chat = createTestChat(1L, "Test Chat");
        User sender = createTestUser(1L, ADMIN_USERNAME);

        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));

        // Act
        systemMessageService.createUserRemovedMessage(chat, USERNAME, ADMIN_USERNAME);

        // Assert
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getMessageType()).isEqualTo(MessageSystemType.USER_REMOVED);
        assertThat(savedMessage.getText()).isEqualTo("Пользователь testuser удален из чата пользователем adminuser");
    }

    @Test
    void createUserLeftMessage_WithValidData_ShouldCreateCorrectMessage() {
        // Arrange
        Chat chat = createTestChat(1L, "Test Chat");
        User sender = createTestUser(1L, USERNAME);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(sender));

        // Act
        systemMessageService.createUserLeftMessage(chat, USERNAME);

        // Assert
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getMessageType()).isEqualTo(MessageSystemType.USER_LEFT);
        assertThat(savedMessage.getText()).isEqualTo("Пользователь testuser вышел из чата");
        assertThat(savedMessage.getSender()).isEqualTo(sender);
    }

    @Test
    void createUserPromotedMessage_WithValidData_ShouldCreateCorrectMessage() {
        // Arrange
        Chat chat = createTestChat(1L, "Test Chat");
        User sender = createTestUser(1L, ADMIN_USERNAME);

        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));

        // Act
        systemMessageService.createUserPromotedMessage(chat, USERNAME, ADMIN_USERNAME);

        // Assert
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getMessageType()).isEqualTo(MessageSystemType.USER_PROMOTED);
        assertThat(savedMessage.getText()).isEqualTo("Пользователь testuser назначен администратором пользователем adminuser");
    }

    @Test
    void createUserDemotedMessage_WithValidData_ShouldCreateCorrectMessage() {
        // Arrange
        Chat chat = createTestChat(1L, "Test Chat");
        User sender = createTestUser(1L, ADMIN_USERNAME);

        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));

        // Act
        systemMessageService.createUserDemotedMessage(chat, USERNAME, ADMIN_USERNAME);

        // Assert
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getMessageType()).isEqualTo(MessageSystemType.USER_DEMOTED);
        assertThat(savedMessage.getText()).isEqualTo("Пользователь testuser лишен прав администратора пользователем adminuser");
    }

    @Test
    void createGroupNameChangedMessage_WithValidData_ShouldCreateCorrectMessage() {
        // Arrange
        Chat chat = createTestChat(1L, "New Title");
        User sender = createTestUser(1L, ADMIN_USERNAME);
        String oldName = "Old Title";
        String newName = "New Title";

        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));

        // Act
        systemMessageService.createGroupNameChangedMessage(chat, oldName, newName, ADMIN_USERNAME);

        // Assert
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getMessageType()).isEqualTo(MessageSystemType.GROUP_TITLE_CHANGED);
        assertThat(savedMessage.getText()).isEqualTo("Пользователь adminuser изменил название группы с \"Old Title\" на \"New Title\"");
    }

    @Test
    void createGroupPhotoChangedMessage_WithValidData_ShouldCreateCorrectMessage() {
        // Arrange
        Chat chat = createTestChat(1L, "Test Chat");
        User sender = createTestUser(1L, ADMIN_USERNAME);

        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));

        // Act
        systemMessageService.createGroupPhotoChangedMessage(chat, ADMIN_USERNAME);

        // Assert
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getMessageType()).isEqualTo(MessageSystemType.GROUP_PHOTO_CHANGED);
        assertThat(savedMessage.getText()).isEqualTo("Пользователь adminuser изменил фотографию группы");
    }

    // ===== ТЕСТЫ ДЛЯ ОШИБОЧНЫХ СЦЕНАРИЕВ =====

    @Test
    void createSystemMessage_WhenSenderNotFound_ShouldThrowException() {
        // Arrange
        Chat chat = createTestChat(1L, "Test Chat");

        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> systemMessageService.createUserAddedMessage(chat, USERNAME, ADMIN_USERNAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь не найден: " + ADMIN_USERNAME);

        verify(messageRepository, never()).save(any());
    }

    // ===== ТЕСТЫ ДЛЯ ВСПОМОГАТЕЛЬНЫХ МЕТОДОВ =====

    @Test
    void getTemplate_WithExistingType_ShouldReturnTemplate() {
        // Act
        String template = systemMessageService.getTemplate(MessageSystemType.USER_ADDED);

        // Assert
        assertThat(template).isEqualTo("Пользователь %s добавлен в чат пользователем %s");
    }

    @Test
    void getTemplate_WithNonExistingType_ShouldReturnNull() {
        // Act
        String template = systemMessageService.getTemplate(MessageSystemType.REGULAR);

        // Assert
        assertThat(template).isNull();
    }

    @Test
    void validateTemplates_OnServiceCreation_ShouldNotThrowException() {
        // Этот тест проверяет что сервис успешно создается
        // Если валидация провалится - сервис не создастся и тест упадет
        assertThat(systemMessageService).isNotNull();
    }

    // ===== ТЕСТЫ ДЛЯ ФОРМАТИРОВАНИЯ ТЕКСТА =====

    @Test
    void createSystemMessage_WithSpecialCharacters_ShouldFormatCorrectly() {
        // Arrange
        Chat chat = createTestChat(1L, "Test Chat");
        User sender = createTestUser(1L, "user-name");

        when(userRepository.findByUsername("user-name")).thenReturn(Optional.of(sender));

        // Act
        systemMessageService.createUserAddedMessage(chat, "test.user", "user-name");

        // Assert
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getText()).isEqualTo("Пользователь test.user добавлен в чат пользователем user-name");
    }

    @Test
    void createSystemMessage_WithEmptyStrings_ShouldFormatCorrectly() {
        // Arrange
        Chat chat = createTestChat(1L, "");
        User sender = createTestUser(1L, ADMIN_USERNAME);

        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));

        // Act
        systemMessageService.createGroupNameChangedMessage(chat, "", "", ADMIN_USERNAME);

        // Assert
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getText()).isEqualTo("Пользователь adminuser изменил название группы с \"\" на \"\"");
    }

    // ===== ТЕСТ НА КОРРЕКТНОСТЬ ВСЕХ ШАБЛОНОВ =====

    @Test
    void allMessageTypes_ExceptRegular_ShouldHaveTemplates() {
        // Этот тест проверяет что для всех MessageSystemType (кроме REGULAR) есть шаблоны
        for (MessageSystemType type : MessageSystemType.values()) {
            if (type != MessageSystemType.REGULAR) {
                String template = systemMessageService.getTemplate(type);
                assertThat(template)
                        .as("Template for message type " + type + " should not be null")
                        .isNotNull()
                        .as("Template for message type " + type + " should not be empty")
                        .isNotEmpty();
            }
        }
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    private Chat createTestChat(Long id, String title) {
        Chat chat = new Chat();
        chat.setId(id);
        return chat;
    }

    private User createTestUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }
}