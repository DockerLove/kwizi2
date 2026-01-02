package com.example.kwizi.service;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.response.ChatHistoryResponse;
import com.example.kwizi.enums.ChatRole;
import com.example.kwizi.enums.ChatType;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.MessageService.MessageEditTimeExpiredException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageService тесты")
class ChatMessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ChatRepository chatRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private NotificationService notificationService;
    @Mock private ChatService chatService;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private User testUser;
    private User testRecipient;
    private Chat testChat;
    private Message testMessage;
    private ChatMember testChatMember;
    private MessageDto messageDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        testRecipient = new User();
        testRecipient.setId(2L);
        testRecipient.setUsername("testRecipient");

        testChat = new Chat();
        testChat.setId(1L);
        testChat.setChatType(ChatType.PRIVATE);

        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setChat(testChat);
        testMessage.setSender(testUser);
        testMessage.setText("Original text");
        testMessage.setCreatedAt(OffsetDateTime.now().minusHours(1));

        testChatMember = new ChatMember(testChat, testUser, ChatRole.MEMBER);

        messageDto = new MessageDto();
        messageDto.setChatId(1L);
        messageDto.setText("Test message");
    }
    
    @Nested
    @DisplayName("Отправка сообщений")
    class SendMessage {

        @Nested
        @DisplayName("Обычная отправка в чат")
        class InChat {

            @Test
            @DisplayName("Успешная отправка при валидных данных")
            void success() {
                
                when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(chatMemberRepository.existsByChatIdAndUserId(1L, 1L)).thenReturn(true);
                when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

                
                Message result = chatMessageService.sendMessage(messageDto, 1L);

                
                assertThat(result).isNotNull();
                verify(chatRepository).findById(1L);
                verify(userRepository).findById(1L);
                verify(chatMemberRepository).existsByChatIdAndUserId(1L, 1L);
                verify(messageRepository).save(any(Message.class));
                verify(chatService).updateChatActivity(1L);
            }

            @Test
            @DisplayName("Чат не найден")
            void chatNotFound() {
                
                when(chatRepository.findById(1L)).thenReturn(Optional.empty());

                
                assertThatThrownBy(() -> chatMessageService.sendMessage(messageDto, 1L))
                        .isInstanceOf(ChatNotFoundException.class);

                verify(userRepository, never()).findById(anyLong());
                verify(messageRepository, never()).save(any(Message.class));
            }

            @Test
            @DisplayName("Пользователь не найден")
            void userNotFound() {
                
                when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
                when(userRepository.findById(1L)).thenReturn(Optional.empty());

                
                assertThatThrownBy(() -> chatMessageService.sendMessage(messageDto, 1L))
                        .isInstanceOf(UserNotFoundException.class);

                verify(messageRepository, never()).save(any(Message.class));
            }

            @Test
            @DisplayName("Пользователь не состоит в чате")
            void userNotInChat() {
                
                when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(chatMemberRepository.existsByChatIdAndUserId(1L, 1L)).thenReturn(false);

                
                assertThatThrownBy(() -> chatMessageService.sendMessage(messageDto, 1L))
                        .isInstanceOf(SecurityException.class);

                verify(messageRepository, never()).save(any(Message.class));
            }
        }

        @Nested
        @DisplayName("Отправка приватного сообщения")
        class PrivateMessage {

            @Test
            @DisplayName("Создаёт новый чат, если не существует")
            void createsNewChat() {
                
                MessageDto dto = new MessageDto();
                dto.setText("Private message");

                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(userRepository.findById(2L)).thenReturn(Optional.of(testRecipient));
                when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L)).thenReturn(Optional.empty());
                when(chatRepository.save(any(Chat.class))).thenReturn(testChat);
                when(chatMemberRepository.saveAll(anyList())).thenReturn(List.of(testChatMember));
                when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

                
                Message result = chatMessageService.sendPrivateMessage(dto, 1L, 2L);

                
                assertThat(result).isNotNull();
                verify(chatRepository).save(any(Chat.class));
                verify(chatMemberRepository).saveAll(anyList());
                verify(chatService).updateChatActivity(testChat.getId());
            }

            @Test
            @DisplayName("Использует существующий чат")
            void usesExistingChat() {
                
                MessageDto dto = new MessageDto();
                dto.setText("Private message");

                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(userRepository.findById(2L)).thenReturn(Optional.of(testRecipient));
                when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L)).thenReturn(Optional.of(1L));
                when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
                when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

                
                Message result = chatMessageService.sendPrivateMessage(dto, 1L, 2L);

                
                assertThat(result).isNotNull();
                verify(chatRepository, never()).save(any(Chat.class));
                verify(chatService).updateChatActivity(1L);
            }
        }
    }

    @Nested
    @DisplayName("Получение истории чата")
    class GetChatHistory {

        @Test
        @DisplayName("Успешное получение истории при наличии доступа")
        void success() {
            
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")));
            Page<Message> messagePage = new PageImpl<>(List.of(testMessage));

            when(chatMemberRepository.existsByChatIdAndUsername(1L, "testUser")).thenReturn(true);
            when(messageRepository.findByChatId(1L, pageable)).thenReturn(messagePage);

            
            Page<ChatHistoryResponse> result = chatMessageService.getChatHistory(1L, 0, 10, "desc", "testUser");

            
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(chatMemberRepository).existsByChatIdAndUsername(1L, "testUser");
            verify(messageRepository).findByChatId(1L, pageable);
        }

        @Test
        @DisplayName("Отказано в доступе, если пользователь не в чате")
        void accessDenied() {
            
            when(chatMemberRepository.existsByChatIdAndUsername(1L, "unauthorizedUser")).thenReturn(false);

            
            assertThatThrownBy(() -> chatMessageService.getChatHistory(1L, 0, 10, "desc", "unauthorizedUser"))
                    .isInstanceOf(AccessDeniedException.class);

            verify(messageRepository, never()).findByChatId(anyLong(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Редактирование сообщения")
    class EditMessage {

        @Test
        @DisplayName("Успешное редактирование (владелец, в сроке)")
        void success() {
            
            String newText = "Updated text";
            testMessage.setCreatedAt(OffsetDateTime.now().minusHours(12)); // < 24h

            when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

            
            chatMessageService.editMessage(1L, newText, "testUser");

            
            assertThat(testMessage.getText()).isEqualTo(newText);
            assertThat(testMessage.isEdited()).isTrue();
            verify(notificationService).notifyMessageEdited(1L, 1L, newText, "testUser");
        }

        @Test
        @DisplayName("Попытка редактировать чужое сообщение")
        void notOwner() {
            
            when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));

            
            assertThatThrownBy(() -> chatMessageService.editMessage(1L, "new", "otherUser"))
                    .isInstanceOf(AccessDeniedException.class);

            verify(messageRepository, never()).save(any(Message.class));
            verify(notificationService, never()).notifyMessageEdited(anyLong(), anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Превышен лимит времени редактирования (24ч)")
        void timeExpired() {
            
            testMessage.setCreatedAt(OffsetDateTime.now().minusHours(25)); // > 24h
            when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));

            
            assertThatThrownBy(() -> chatMessageService.editMessage(1L, "new", "testUser"))
                    .isInstanceOf(MessageEditTimeExpiredException.class);

            verify(messageRepository, never()).save(any(Message.class));
        }
    }

    @Nested
    @DisplayName("Удаление сообщения")
    class DeleteMessage {

        @Test
        @DisplayName("Владелец удаляет своё сообщение")
        void senderDeletes() {
            
            when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
            when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
            when(chatMemberRepository.findByChatIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testChatMember));

            
            chatMessageService.deleteMessage(1L, "testUser");

            
            verify(messageRepository).delete(testMessage);
            verify(notificationService).notifyMessageDeleted(1L, 1L, "testUser");
        }

        @Test
        @DisplayName("Админ удаляет сообщение")
        void adminDeletes() {
            
            User admin = new User();
            admin.setId(3L);
            admin.setUsername("adminUser");
            ChatMember adminMember = new ChatMember(testChat, admin, ChatRole.ADMIN);

            when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
            when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
            when(chatMemberRepository.findByChatIdAndUserId(1L, 3L))
                    .thenReturn(Optional.of(adminMember));

            
            chatMessageService.deleteMessage(1L, "adminUser");

            
            verify(messageRepository).delete(testMessage);
            verify(notificationService).notifyMessageDeleted(1L, 1L, "adminUser");
        }

        @Test
        @DisplayName("Обычный участник без прав пытается удалить")
        void noPermission() {
            
            User other = new User();
            other.setId(3L);
            other.setUsername("otherUser");
            ChatMember member = new ChatMember(testChat, other, ChatRole.MEMBER);

            when(userRepository.findByUsername("otherUser")).thenReturn(Optional.of(other));
            when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
            when(chatMemberRepository.findByChatIdAndUserId(1L, 3L))
                    .thenReturn(Optional.of(member));

            
            assertThatThrownBy(() -> chatMessageService.deleteMessage(1L, "otherUser"))
                    .isInstanceOf(AccessDeniedException.class);

            verify(messageRepository, never()).delete(any(Message.class));
            verify(notificationService, never()).notifyMessageDeleted(anyLong(), anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("Получение участников чата")
    class GetChatMembers {

        @Test
        @DisplayName("Успешное получение списка участников")
        void success() {
            
            List<Long> expected = List.of(1L, 2L, 3L);
            when(chatRepository.existsById(1L)).thenReturn(true);
            when(chatMemberRepository.findUserIdsByChatId(1L)).thenReturn(expected);

            
            List<Long> result = chatMessageService.getChatMembers(1L);

            
            assertThat(result).isEqualTo(expected);
            verify(chatRepository).existsById(1L);
            verify(chatMemberRepository).findUserIdsByChatId(1L);
        }

        @Test
        @DisplayName("Чат не найден")
        void chatNotFound() {
            
            when(chatRepository.existsById(1L)).thenReturn(false);

            
            assertThatThrownBy(() -> chatMessageService.getChatMembers(1L))
                    .isInstanceOf(ChatNotFoundException.class);

            verify(chatMemberRepository, never()).findUserIdsByChatId(anyLong());
        }
    }

    @Nested
    @DisplayName("Конвертация сообщения")
    class Conversion {

        @Test
        @DisplayName("Корректная конвертация в ChatHistoryResponse")
        void convertToChatHistoryResponse() {
            
            ChatHistoryResponse response = chatMessageService.convertToChatHistoryResponse(testMessage);

            
            assertThat(response.getMessageId()).isEqualTo(testMessage.getId());
            assertThat(response.getText()).isEqualTo(testMessage.getText());
            assertThat(response.getSenderId()).isEqualTo(testMessage.getSender().getId());
            assertThat(response.getSenderName()).isEqualTo(testMessage.getSender().getUsername());
            assertThat(response.getCreatedAt()).isEqualTo(testMessage.getCreatedAt().toLocalDateTime());
        }
    }
}