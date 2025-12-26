package com.example.kwizi.service;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.enums.ChatRole;
import com.example.kwizi.enums.ChatType;
import com.example.kwizi.exception.BusinessLogicException;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.ChatService.ChatOperationNotAllowedException;
import com.example.kwizi.exception.ChatService.DuplicateChatMemberException;
import com.example.kwizi.exception.ChatService.NotGroupChatException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.GroupChat;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService тесты")
class ChatServiceTest {

    @Mock private ChatRepository chatRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private SystemMessageService systemMessageService;
    @Mock private NotificationService notificationService;
    @Mock private FileStorageService fileStorageService;
    @Mock private MultipartFile multipartFile;

    @InjectMocks
    private ChatService chatService;

    private User creator;
    private User member1;
    private User member2;
    private Chat groupChat;
    private Chat privateChat;

    @BeforeEach
    void setUp() {
        creator = createUser(1L, "creator");
        member1 = createUser(2L, "member1");
        member2 = createUser(3L, "member2");

        groupChat = createGroupChat(100L);
        privateChat = createPrivateChat(200L);
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private Chat createGroupChat(Long id) {
        Chat chat = new Chat();
        chat.setId(id);
        chat.setChatType(ChatType.GROUP);
        chat.setCreatedAt(OffsetDateTime.now());
        chat.setLastActivityAt(OffsetDateTime.now());

        GroupChat groupChatDetails = new GroupChat();
        groupChatDetails.setGroupName("Test Group");
        chat.setGroupChat(groupChatDetails);

        return chat;
    }

    private Chat createPrivateChat(Long id) {
        Chat chat = new Chat();
        chat.setId(id);
        chat.setChatType(ChatType.PRIVATE);
        return chat;
    }

    private ChatMember createChatMember(Long chatId, Long userId, User user, ChatRole role) {
        ChatMember member = new ChatMember();
        member.setId(new ChatMember.ChatMemberId(chatId, userId));
        member.setChat(groupChat);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    // -----------------------------
    // Создание группового чата
    // -----------------------------
    @Nested
    @DisplayName("Создание группового чата")
    class CreateGroupChat {

        @Test
        @DisplayName("✅ Успешное создание группового чата")
        void success() {
            // given
            CreateGroupChatRequest request = new CreateGroupChatRequest();
            request.setGroupName("New Group");
            request.setInitialMemberIds(Arrays.asList(2L, 3L));

            when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
            when(userRepository.findAllById(Arrays.asList(2L, 3L))).thenReturn(Arrays.asList(member1, member2));
            when(chatRepository.save(any(Chat.class))).thenReturn(groupChat);

            // ⚠️ ДОБАВЬТЕ ЭТУ СТРОКУ ⚠️
            when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));

            // when
            chatService.createGroupChat(request, "creator");

            // then
            verify(chatRepository, times(2)).save(any(Chat.class));
            verify(chatMemberRepository, times(2)).save(any(ChatMember.class));
        }

        @Test
        @DisplayName("❌ Пользователь-инициатор не найден")
        void creatorNotFound() {
            // given
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
            CreateGroupChatRequest request = new CreateGroupChatRequest();

            // when & then
            assertThatThrownBy(() -> chatService.createGroupChat(request, "unknown"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("❌ Один из участников не найден")
        void memberNotFound() {
            // given
            CreateGroupChatRequest request = new CreateGroupChatRequest();
            request.setGroupName("Test Group");
            request.setInitialMemberIds(Arrays.asList(2L, 999L));

            when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
            when(userRepository.findAllById(Arrays.asList(2L, 999L))).thenReturn(Arrays.asList(member1));

            when(chatRepository.save(any(Chat.class))).thenReturn(groupChat);
            // when & then
            assertThatThrownBy(() -> chatService.createGroupChat(request, "creator"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("❌ Дублирующиеся участники")
        void duplicateMembers() {
            // given
            CreateGroupChatRequest request = new CreateGroupChatRequest();
            request.setGroupName("Test");
            request.setInitialMemberIds(List.of(2L, 2L));

            when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));

            // when & then
            assertThatThrownBy(() -> chatService.createGroupChat(request, "creator"))
                    .isInstanceOf(DuplicateChatMemberException.class);
        }
    }

    // -----------------------------
    // Создание приватного чата
    // -----------------------------
    @Nested
    @DisplayName("Создание приватного чата")
    class CreatePrivateChat {

        @Test
        @DisplayName("✅ Успешное создание приватного чата")
        void success() {
            // given
            CreatePrivateChatRequest request = new CreatePrivateChatRequest();
            request.setRecipientUsername("member1");

            when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
            when(userRepository.findByUsername("member1")).thenReturn(Optional.of(member1));
            when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L)).thenReturn(Optional.empty());
            when(chatRepository.save(any(Chat.class))).thenReturn(privateChat);
            when(chatRepository.findById(200L)).thenReturn(Optional.of(privateChat));

            // when
            chatService.createPrivateChat(request, "creator");

            // then
            verify(chatRepository, times(2)).save(any(Chat.class));
            verify(chatMemberRepository, times(2)).save(any(ChatMember.class));
        }

        @Test
        @DisplayName("❌ Попытка создать чат с самим собой")
        void chatWithSelfNotAllowed() {
            // given
            CreatePrivateChatRequest request = new CreatePrivateChatRequest();
            request.setRecipientUsername("creator");

            when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));

            // when & then
            assertThatThrownBy(() -> chatService.createPrivateChat(request, "creator"))
                    .isInstanceOf(ChatOperationNotAllowedException.class);
        }

        @Test
        @DisplayName("❌ Приватный чат уже существует")
        void chatAlreadyExists() {
            // given
            CreatePrivateChatRequest request = new CreatePrivateChatRequest();
            request.setRecipientUsername("member1");

            when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
            when(userRepository.findByUsername("member1")).thenReturn(Optional.of(member1));
            when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L)).thenReturn(Optional.of(300L));

            // when & then
            assertThatThrownBy(() -> chatService.createPrivateChat(request, "creator"))
                    .isInstanceOf(DuplicateChatMemberException.class);
        }
    }

    // -----------------------------
    // Управление участниками чата
    // -----------------------------
    @Nested
    @DisplayName("Управление участниками чата")
    class ManageChatMembers {

        @Nested
        @DisplayName("Добавление участника")
        class AddMember {

            @Test
            @DisplayName("✅ Успешное добавление участника в групповой чат")
            void success() {
                // given
                AddChatMemberRequestDto request = new AddChatMemberRequestDto();
                request.setChatId(100L);
                request.setUserId(3L);

                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
                when(chatMemberRepository.existsByChatIdAndUserId(100L, 3L)).thenReturn(false);

                // when
                chatService.addChatMember(request, "creator");

                // then
                verify(chatMemberRepository).save(any());
                verify(systemMessageService).createUserAddedMessage(any(), any(), any());
            }

            @Test
            @DisplayName("❌ Чат не найден")
            void chatNotFound() {
                // given
                when(chatRepository.findById(999L)).thenReturn(Optional.empty());
                AddChatMemberRequestDto request = new AddChatMemberRequestDto();
                request.setChatId(999L);

                // when & then
                assertThatThrownBy(() -> chatService.addChatMember(request, "creator"))
                        .isInstanceOf(ChatNotFoundException.class);
            }

            @Test
            @DisplayName("❌ Участник уже в чате")
            void memberAlreadyExists() {
                // given
                AddChatMemberRequestDto request = new AddChatMemberRequestDto();
                request.setChatId(100L);
                request.setUserId(2L);

                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
                when(chatMemberRepository.existsByChatIdAndUserId(100L, 2L)).thenReturn(true);

                // when & then
                assertThatThrownBy(() -> chatService.addChatMember(request, "creator"))
                        .isInstanceOf(DuplicateChatMemberException.class);
            }

            @Test
            @DisplayName("❌ Попытка добавить участника в приватный чат")
            void privateChatNotAllowed() {
                // given
                AddChatMemberRequestDto request = new AddChatMemberRequestDto();
                request.setChatId(200L);
                request.setUserId(3L);

                when(chatRepository.findById(200L)).thenReturn(Optional.of(privateChat));
                when(userRepository.findById(3L)).thenReturn(Optional.of(member2));

                // when & then
                assertThatThrownBy(() -> chatService.addChatMember(request, "creator"))
                        .isInstanceOf(ChatOperationNotAllowedException.class);
            }
        }

        @Nested
        @DisplayName("Назначение администратора")
        class SetAdmin {

            @Test
            @DisplayName("✅ Успешное назначение админа (владелец → участник)")
            void success() {
                // given
                ChatMember regular = createChatMember(100L, 2L, member1, ChatRole.MEMBER);
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
                when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 1L)).thenReturn(Optional.of(createChatMember(100L, 1L, creator, ChatRole.OWNER)));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(regular));

                // when
                chatService.setAdmin(100L, 2L, 1L);

                // then
                assertThat(regular.getRole()).isEqualTo(ChatRole.ADMIN);
                verify(systemMessageService).createUserPromotedMessage(any(), any(), any());
                verify(notificationService).notifyUserPromoted(any(), any(), any());
            }

            @Test
            @DisplayName("❌ Пользователь уже админ")
            void userAlreadyAdmin() {
                // given
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
                when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 1L)).thenReturn(Optional.of(createChatMember(100L, 1L, creator, ChatRole.OWNER)));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(createChatMember(100L, 2L, member1, ChatRole.ADMIN)));

                // when & then
                assertThatThrownBy(() -> chatService.setAdmin(100L, 2L, 1L))
                        .isInstanceOf(BusinessLogicException.class);
            }

            @Test
            @DisplayName("❌ Только владелец может назначать админов")
            void onlyOwnerCanPromote() {
                // given
                ChatMember requester = createChatMember(100L, 2L, member1, ChatRole.MEMBER);
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
                when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(requester));

                // when & then
                assertThatThrownBy(() -> chatService.setAdmin(100L, 3L, 2L))
                        .isInstanceOf(AccessDeniedException.class);
            }
        }

        @Nested
        @DisplayName("Исключение участника")
        class RemoveMember {

            @Test
            @DisplayName("✅ Владелец исключает участника")
            void ownerRemovesMember() {
                // given
                ChatMember regular = createChatMember(100L, 3L, member2, ChatRole.MEMBER);
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
                when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 1L)).thenReturn(Optional.of(createChatMember(100L, 1L, creator, ChatRole.OWNER)));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 3L)).thenReturn(Optional.of(regular));

                // when
                chatService.removeChatMember(100L, 3L, 1L);

                // then
                verify(chatMemberRepository).delete(regular);
            }

            @Test
            @DisplayName("❌ Владелец не может исключить себя")
            void ownerCantRemoveSelf() {
                // given
                ChatMember owner = createChatMember(100L, 1L, creator, ChatRole.OWNER);
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 1L)).thenReturn(Optional.of(owner));

                // when & then
                assertThatThrownBy(() -> chatService.removeChatMember(100L, 1L, 1L))
                        .isInstanceOf(BusinessLogicException.class);
            }

            @Test
            @DisplayName("✅ Админ исключает участника")
            void adminRemovesMember() {
                // given
                ChatMember admin = createChatMember(100L, 2L, member1, ChatRole.ADMIN);
                ChatMember regular = createChatMember(100L, 3L, member2, ChatRole.MEMBER);
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
                when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(admin));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 3L)).thenReturn(Optional.of(regular));

                // when
                chatService.removeChatMember(100L, 3L, 2L);

                // then
                verify(chatMemberRepository).delete(regular);
            }

            @Test
            @DisplayName("❌ Обычный участник не может исключать других")
            void memberCannotRemove() {
                // given
                ChatMember requester = createChatMember(100L, 3L, member2, ChatRole.MEMBER);
                ChatMember target = createChatMember(100L, 2L, member1, ChatRole.MEMBER);
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
                when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 3L)).thenReturn(Optional.of(requester));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(target));

                // when & then
                assertThatThrownBy(() -> chatService.removeChatMember(100L, 2L, 3L))
                        .isInstanceOf(AccessDeniedException.class);
            }
        }

        @Nested
        @DisplayName("Выход из чата")
        class LeaveChat {

            @Test
            @DisplayName("✅ Участник покидает групповой чат")
            void success() {
                // given
                ChatMember leaver = createChatMember(100L, 2L, member1, ChatRole.MEMBER);
                ChatMember owner = createChatMember(100L, 1L, creator, ChatRole.OWNER);
                Set<ChatMember> members = Set.of(owner, leaver);
                groupChat.setChatMembers(members);

                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(leaver));

                // when
                chatService.leaveChat(100L, 2L);

                // then
                verify(chatMemberRepository).delete(leaver);
                verify(systemMessageService).createUserLeftMessage(any(), any());
            }

            @Test
            @DisplayName("❌ Владелец не может покинуть чат")
            void ownerCantLeave() {
                // given
                ChatMember owner = createChatMember(100L, 1L, creator, ChatRole.OWNER);
                groupChat.setChatMembers(Set.of(owner));

                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 1L)).thenReturn(Optional.of(owner));

                // when & then
                assertThatThrownBy(() -> chatService.leaveChat(100L, 1L))
                        .isInstanceOf(BusinessLogicException.class);
            }

            @Test
            @DisplayName("❌ Нельзя покинуть приватный чат через этот метод")
            void privateChatNotAllowed() {
                // given
                when(chatRepository.findById(200L)).thenReturn(Optional.of(privateChat));
                when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

                // when & then
                assertThatThrownBy(() -> chatService.leaveChat(200L, 1L))
                        .isInstanceOf(NotGroupChatException.class);
            }
        }
    }

    // -----------------------------
    // Обновление настроек чата
    // -----------------------------
    @Nested
    @DisplayName("Обновление настроек чата")
    class UpdateChatSettings {

        @Nested
        @DisplayName("Изменение названия группы")
        class UpdateGroupName {

            @Test
            @DisplayName("✅ Владелец успешно меняет название")
            void success() {
                // given
                String newName = "New Group Name";
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 1L))
                        .thenReturn(Optional.of(createChatMember(100L, 1L, creator, ChatRole.OWNER)));

                // when
                chatService.updateGroupName(100L, newName, 1L);

                // then
                assertThat(groupChat.getGroupChat().getGroupName()).isEqualTo(newName);
                verify(chatRepository, times(2)).save(groupChat);
                verify(systemMessageService).createGroupNameChangedMessage(any(), any(), any(), any());
                verify(notificationService).notifyGroupNameChanged(any(), any(), any(), any());
            }

            @Test
            @DisplayName("❌ Нельзя изменить название приватного чата")
            void privateChatNotAllowed() {
                // given
                when(chatRepository.findById(200L)).thenReturn(Optional.of(privateChat));

                // when & then
                assertThatThrownBy(() -> chatService.updateGroupName(200L, "New", 1L))
                        .isInstanceOf(BusinessLogicException.class);
            }

            @Test
            @DisplayName("❌ Только владелец или админ могут менять название")
            void onlyOwnerOrAdmin() {
                // given
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 3L))
                        .thenReturn(Optional.of(createChatMember(100L, 3L, member2, ChatRole.MEMBER)));

                // when & then
                assertThatThrownBy(() -> chatService.updateGroupName(100L, "New", 3L))
                        .isInstanceOf(AccessDeniedException.class);
            }
        }

        @Nested
        @DisplayName("Изменение аватара чата")
        class UpdateAvatar {

            @Test
            @DisplayName("✅ Владелец успешно меняет аватар")
            void success() {
                // given
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 1L))
                        .thenReturn(Optional.of(createChatMember(100L, 1L, creator, ChatRole.OWNER)));
                when(multipartFile.isEmpty()).thenReturn(false);
                when(fileStorageService.saveChatAvatar(multipartFile, 100L)).thenReturn("avatar-url");

                // when
                chatService.updateChatAvatar(100L, multipartFile, 1L);

                // then
                assertThat(groupChat.getGroupChat().getAvatarUrl()).isEqualTo("avatar-url");
                verify(chatRepository, times(2)).save(groupChat);
                verify(systemMessageService).createGroupPhotoChangedMessage(any(), any());
                verify(notificationService).notifyGroupPhotoChanged(any(), any());
            }

            @Test
            @DisplayName("❌ Пустой файл")
            void emptyFile() {
                // given
                when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
                when(chatMemberRepository.findByChatIdAndUserId(100L, 1L))
                        .thenReturn(Optional.of(createChatMember(100L, 1L, creator, ChatRole.OWNER)));
                when(multipartFile.isEmpty()).thenReturn(true);

                // when & then
                assertThatThrownBy(() -> chatService.updateChatAvatar(100L, multipartFile, 1L))
                        .isInstanceOf(BusinessLogicException.class);
            }

            @Test
            @DisplayName("❌ Приватный чат не поддерживает аватар")
            void privateChatNotAllowed() {
                // given
                when(chatRepository.findById(200L)).thenReturn(Optional.of(privateChat));
                when(chatMemberRepository.findByChatIdAndUserId(200L, 1L))
                        .thenReturn(Optional.of(createChatMember(200L, 1L, creator, ChatRole.MEMBER)));

                // when & then
                assertThatThrownBy(() -> chatService.updateChatAvatar(200L, multipartFile, 1L))
                        .isInstanceOf(BusinessLogicException.class);
            }
        }
    }

    // -----------------------------
    // Обновление активности
    // -----------------------------
    @Nested
    @DisplayName("Обновление активности чата")
    class UpdateActivity {

        @Test
        @DisplayName("✅ Успешное обновление активности")
        void success() {
            // given
            OffsetDateTime oldTime = groupChat.getLastActivityAt();
            when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));

            // when
            chatService.updateChatActivity(100L);

            // then
            verify(chatRepository).save(groupChat);
            assertThat(groupChat.getLastActivityAt()).isAfter(oldTime);
        }

        @Test
        @DisplayName("❌ Чат не найден")
        void chatNotFound() {
            // given
            when(chatRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatService.updateChatActivity(999L))
                    .isInstanceOf(ChatNotFoundException.class);
        }
    }
}