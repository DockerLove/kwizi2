package com.example.kwizi.service;

import com.example.kwizi.enums.MessageSystemType;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemMessageService тесты")
class SystemMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SystemMessageService systemMessageService;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    private final Long CHAT_ID = 1L;
    private final Long USER_ID = 1L;
    private final Long ADMIN_ID = 2L;
    private final String USERNAME = "testuser";
    private final String ADMIN_USERNAME = "adminuser";

    @Nested
    @DisplayName("Основные сценарии создания системных сообщений")
    class MainMessageCreationScenarios {

        @Test
        @DisplayName("Создание сообщения о добавлении пользователя в чат")
        void createUserAddedMessage_WithValidData_ShouldCreateCorrectMessage() {
            
            Chat chat = createTestChat(CHAT_ID);
            User sender = createTestUser(ADMIN_ID, ADMIN_USERNAME);

            when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));
            
            systemMessageService.createUserAddedMessage(chat, USERNAME, ADMIN_USERNAME);
            
            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();

            assertThat(savedMessage)
                    .as("Должно сохраниться корректное сообщение")
                    .extracting(
                            Message::getMessageType,
                            Message::getText,
                            Message::getChat,
                            Message::getSender
                    )
                    .containsExactly(
                            MessageSystemType.USER_ADDED,
                            "Пользователь testuser добавлен в чат пользователем adminuser",
                            chat,
                            sender
                    );

            assertThat(savedMessage.getCreatedAt())
                    .as("Дата создания должна быть установлена")
                    .isNotNull();
        }

        @Test
        @DisplayName("Создание сообщения о удалении пользователя из чата")
        void createUserRemovedMessage_WithValidData_ShouldCreateCorrectMessage() {
            
            Chat chat = createTestChat(CHAT_ID);
            User sender = createTestUser(ADMIN_ID, ADMIN_USERNAME);

            when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));

            
            systemMessageService.createUserRemovedMessage(chat, USERNAME, ADMIN_USERNAME);

            
            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();

            assertThat(savedMessage)
                    .as("Должно сохраниться сообщение о удалении пользователя")
                    .extracting(
                            Message::getMessageType,
                            Message::getText
                    )
                    .containsExactly(
                            MessageSystemType.USER_REMOVED,
                            "Пользователь testuser удален из чата пользователем adminuser"
                    );
        }

        @Test
        @DisplayName("Создание сообщения о выходе пользователя из чата")
        void createUserLeftMessage_WithValidData_ShouldCreateCorrectMessage() {
            
            Chat chat = createTestChat(CHAT_ID);
            User sender = createTestUser(USER_ID, USERNAME);

            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(sender));
            
            systemMessageService.createUserLeftMessage(chat, USERNAME);
            
            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();

            assertThat(savedMessage)
                    .as("Должно сохраниться сообщение о выходе пользователя")
                    .extracting(
                            Message::getMessageType,
                            Message::getText,
                            Message::getSender
                    )
                    .containsExactly(
                            MessageSystemType.USER_LEFT,
                            "Пользователь testuser вышел из чата",
                            sender
                    );
        }

        @Test
        @DisplayName("Создание сообщения о назначении администратора")
        void createUserPromotedMessage_WithValidData_ShouldCreateCorrectMessage() {
            
            Chat chat = createTestChat(CHAT_ID);
            User sender = createTestUser(ADMIN_ID, ADMIN_USERNAME);

            when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));
            
            systemMessageService.createUserPromotedMessage(chat, USERNAME, ADMIN_USERNAME);
            
            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();

            assertThat(savedMessage)
                    .as("Должно сохраниться сообщение о назначении администратора")
                    .extracting(
                            Message::getMessageType,
                            Message::getText
                    )
                    .containsExactly(
                            MessageSystemType.USER_PROMOTED,
                            "Пользователь testuser назначен администратором пользователем adminuser"
                    );
        }

        @Test
        @DisplayName("Создание сообщения о снятии прав администратора")
        void createUserDemotedMessage_WithValidData_ShouldCreateCorrectMessage() {
            
            Chat chat = createTestChat(CHAT_ID);
            User sender = createTestUser(ADMIN_ID, ADMIN_USERNAME);

            when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));
            
            systemMessageService.createUserDemotedMessage(chat, USERNAME, ADMIN_USERNAME);
            
            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();

            assertThat(savedMessage)
                    .as("Должно сохраниться сообщение о снятии прав администратора")
                    .extracting(
                            Message::getMessageType,
                            Message::getText
                    )
                    .containsExactly(
                            MessageSystemType.USER_DEMOTED,
                            "Пользователь testuser лишен прав администратора пользователем adminuser"
                    );
        }

        @Test
        @DisplayName("Создание сообщения об изменении названия группы")
        void createGroupNameChangedMessage_WithValidData_ShouldCreateCorrectMessage() {
            
            Chat chat = createTestChat(CHAT_ID);
            User sender = createTestUser(ADMIN_ID, ADMIN_USERNAME);
            String oldName = "Old Title";
            String newName = "New Title";

            when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));
            
            systemMessageService.createGroupNameChangedMessage(chat, oldName, newName, ADMIN_USERNAME);
            
            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();

            assertThat(savedMessage)
                    .as("Должно сохраниться сообщение об изменении названия группы")
                    .extracting(
                            Message::getMessageType,
                            Message::getText
                    )
                    .containsExactly(
                            MessageSystemType.GROUP_TITLE_CHANGED,
                            "Пользователь adminuser изменил название группы с \"Old Title\" на \"New Title\""
                    );
        }

        @Test
        @DisplayName("Создание сообщения об изменении фотографии группы")
        void createGroupPhotoChangedMessage_WithValidData_ShouldCreateCorrectMessage() {
            
            Chat chat = createTestChat(CHAT_ID);
            User sender = createTestUser(ADMIN_ID, ADMIN_USERNAME);

            when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));
            
            systemMessageService.createGroupPhotoChangedMessage(chat, ADMIN_USERNAME);
            
            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();

            assertThat(savedMessage)
                    .as("Должно сохраниться сообщение об изменении фотографии группы")
                    .extracting(
                            Message::getMessageType,
                            Message::getText
                    )
                    .containsExactly(
                            MessageSystemType.GROUP_PHOTO_CHANGED,
                            "Пользователь adminuser изменил фотографию группы"
                    );
        }
    }

    @Nested
    @DisplayName("Сценарии с ошибками")
    class ErrorScenarios {

        @Test
        @DisplayName("Создание сообщения при отсутствии отправителя")
        void createSystemMessage_WhenSenderNotFound_ShouldThrowException() {
            
            Chat chat = createTestChat(CHAT_ID);

            when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> systemMessageService.createUserAddedMessage(chat, USERNAME, ADMIN_USERNAME))
                    .as("Должно выбросить исключение если отправитель не найден")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Пользователь не найден: " + ADMIN_USERNAME);

            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Тесты шаблонов сообщений")
    class MessageTemplatesTests {

        @Test
        @DisplayName("Получение шаблона для существующего типа сообщения")
        void getTemplate_WithExistingType_ShouldReturnTemplate() {
            
            String template = systemMessageService.getTemplate(MessageSystemType.USER_ADDED);
            
            assertThat(template)
                    .as("Должен вернуть шаблон для USER_ADDED")
                    .isEqualTo("Пользователь %s добавлен в чат пользователем %s");
        }

        @Test
        @DisplayName("Получение шаблона для несистемного типа возвращает null")
        void getTemplate_WithRegularType_ShouldReturnNull() {
            
            String template = systemMessageService.getTemplate(MessageSystemType.REGULAR);

            assertThat(template)
                    .as("Для REGULAR типа должен возвращать null")
                    .isNull();
        }

        @ParameterizedTest
        @EnumSource(
                value = MessageSystemType.class,
                names = {"USER_ADDED", "USER_REMOVED", "USER_LEFT", "USER_PROMOTED",
                        "USER_DEMOTED", "GROUP_TITLE_CHANGED", "GROUP_PHOTO_CHANGED"}
        )
        @DisplayName("Все системные типы сообщений имеют шаблоны")
        void allSystemMessageTypes_ShouldHaveTemplates(MessageSystemType type) {
            
            String template = systemMessageService.getTemplate(type);
            
            assertThat(template)
                    .as("Шаблон для типа " + type + " не должен быть null")
                    .isNotNull()
                    .as("Шаблон для типа " + type + " не должен быть пустым")
                    .isNotEmpty()
                    .as("Шаблон для типа " + type + " должен содержать placeholders")
                    .contains("%s");
        }
    }

    @Nested
    @DisplayName("Тесты форматирования текста")
    class TextFormattingTests {

        @Test
        @DisplayName("Форматирование с специальными символами в username")
        void createSystemMessage_WithSpecialCharactersInUsername_ShouldFormatCorrectly() {
            
            Chat chat = createTestChat(CHAT_ID);
            User sender = createTestUser(USER_ID, "user-name");

            when(userRepository.findByUsername("user-name")).thenReturn(Optional.of(sender));

            systemMessageService.createUserAddedMessage(chat, "test.user", "user-name");
            
            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();

            assertThat(savedMessage.getText())
                    .as("Должно корректно форматировать username со специальными символами")
                    .isEqualTo("Пользователь test.user добавлен в чат пользователем user-name");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Рабочая группа",
                "Team 2024",
                "Группа-проекта",
                "Отдел (Разработка)"
        })
        @DisplayName("Форматирование с реальными названиями групп")
        void createGroupNameChangedMessage_WithRealisticNames_ShouldFormatCorrectly(String groupName) {
            
            Chat chat = createTestChat(CHAT_ID);
            User sender = createTestUser(ADMIN_ID, ADMIN_USERNAME);

            when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(sender));
            
            systemMessageService.createGroupNameChangedMessage(
                    chat, "Старое название", groupName, ADMIN_USERNAME
            );

            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();

            String expectedText = String.format(
                    "Пользователь adminuser изменил название группы с \"%s\" на \"%s\"",
                    "Старое название", groupName
            );

            assertThat(savedMessage.getText())
                    .as("Должно корректно форматировать для названия: " + groupName)
                    .isEqualTo(expectedText);
        }
    }

    @Nested
    @DisplayName("Проверка валидации и инициализации")
    class ValidationAndInitializationTests {

        @Test
        @DisplayName("Сервис успешно создается и проходит валидацию шаблонов")
        void validateTemplates_OnServiceCreation_ShouldNotThrowException() {
             
            assertThat(systemMessageService)
                    .as("Сервис должен успешно создаваться")
                    .isNotNull();
        }

        @Test
        @DisplayName("Проверка что все не-REGULAR типы имеют шаблоны")
        void allNonRegularMessageTypes_ShouldHaveTemplates() {
             
            for (MessageSystemType type : MessageSystemType.values()) {
                if (type != MessageSystemType.REGULAR) {
                    String template = systemMessageService.getTemplate(type);
                    assertThat(template)
                            .as("Тип " + type + " должен иметь шаблон")
                            .isNotNull()
                            .isNotEmpty();
                }
            }
        }
    }

    private Chat createTestChat(Long id) {
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