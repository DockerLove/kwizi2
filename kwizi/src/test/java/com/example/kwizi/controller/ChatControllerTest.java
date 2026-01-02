package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.DTO.request.UpdateGroupNameRequest;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.DTO.response.ChatPreviewResponse;
import com.example.kwizi.enums.ChatType;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("ChatController тесты")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private UserDetailsImpl userDetails;

    @InjectMocks
    private ChatController chatController;

    private static final String TEST_USERNAME = "testUser";
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_CHAT_ID = 100L;
    private static final Long TEST_OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
        when(userDetails.getId()).thenReturn(TEST_USER_ID);
    }

    private ChatPreviewResponse createChatPreviewDto() {
        ChatPreviewResponse chatPreviewResponse = new ChatPreviewResponse();
        chatPreviewResponse.setId(TEST_CHAT_ID);
        chatPreviewResponse.setChatType(ChatType.GROUP);
        chatPreviewResponse.setDisplayName("Test chat");
        return chatPreviewResponse;
    }

    private Page<ChatPreviewResponse> createChatPreviewPage(int page, int size) {
        List<ChatPreviewResponse> chats = Collections.singletonList(createChatPreviewDto());
        return new PageImpl<>(chats, PageRequest.of(page, size), 1);
    }

    @SuppressWarnings("unchecked")
    private <T> ApiResponse<T> extractApiResponse(ResponseEntity<?> responseEntity) {
        return (ApiResponse<T>) responseEntity.getBody();
    }

    @Nested
    @DisplayName("Создание чатов")
    class CreateChatTests {

        @Test
        @DisplayName("Успешное создание группового чата")
        void createGroupChat_Success() {
            CreateGroupChatRequest request = new CreateGroupChatRequest();
            request.setGroupName("Новая группа");
            request.setInitialMemberIds(List.of(2L, 3L));

            doNothing().when(chatService)
                    .createGroupChat(eq(request), eq(TEST_USERNAME));

            ResponseEntity<?> response = chatController.createGroupChat(request, userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<?> apiResponse = extractApiResponse(response);
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Групповой чат успешно создан");

            verify(chatService, times(1))
                    .createGroupChat(request, TEST_USERNAME);
        }

        @Test
        @DisplayName("Успешное создание приватного чата")
        void createPrivateChat_Success() {
            CreatePrivateChatRequest request = new CreatePrivateChatRequest();
            request.setRecipientUsername("recipientUser");

            doNothing().when(chatService)
                    .createPrivateChat(eq(request), eq(TEST_USERNAME));

            ResponseEntity<?> response = chatController.createPrivateChat(request, userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<?> apiResponse = extractApiResponse(response);
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Приватный чат успешно создан");

            verify(chatService, times(1))
                    .createPrivateChat(request, TEST_USERNAME);
        }

        @Test
        @DisplayName("Создание группового чата с пустым названием")
        void createGroupChat_EmptyName_ThrowsException() {
            CreateGroupChatRequest request = new CreateGroupChatRequest();
            request.setGroupName("");
            request.setInitialMemberIds(List.of(2L));

            doThrow(new IllegalArgumentException("Название группы не может быть пустым"))
                    .when(chatService).createGroupChat(eq(request), eq(TEST_USERNAME));

            assertThatThrownBy(() -> chatController.createGroupChat(request, userDetails))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Название группы не может быть пустым");
        }
    }

    @Nested
    @DisplayName("Управление участниками чата")
    class MemberManagementTests {

        @Test
        @DisplayName("Успешное добавление участника в чат")
        void addChatMember_Success() {
            AddChatMemberRequestDto request = new AddChatMemberRequestDto();
            request.setUserId(TEST_OTHER_USER_ID);

            doNothing().when(chatService)
                    .addChatMember(any(AddChatMemberRequestDto.class), eq(TEST_USERNAME));

            ResponseEntity<?> response = chatController.addChatMember(TEST_CHAT_ID, request, userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<?> apiResponse = extractApiResponse(response);
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Пользователь успешно добавлен в чат");

            verify(chatService, times(1))
                    .addChatMember(argThat(dto ->
                            dto.getChatId().equals(TEST_CHAT_ID) &&
                                    dto.getUserId().equals(TEST_OTHER_USER_ID)
                    ), eq(TEST_USERNAME));
        }

        @Test
        @DisplayName("Успешное назначение администратора")
        void setAdmin_Success() {
            doNothing().when(chatService)
                    .setAdmin(eq(TEST_CHAT_ID), eq(TEST_OTHER_USER_ID), eq(TEST_USER_ID));

            ResponseEntity<?> response = chatController.setAdmin(TEST_CHAT_ID, TEST_OTHER_USER_ID, userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<?> apiResponse = extractApiResponse(response);
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Пользователь успешно назначен администратором");

            verify(chatService, times(1))
                    .setAdmin(TEST_CHAT_ID, TEST_OTHER_USER_ID, TEST_USER_ID);
        }

        @Test
        @DisplayName("Успешное удаление участника из чата")
        void removeChatMember_Success() {
            doNothing().when(chatService)
                    .removeChatMember(eq(TEST_CHAT_ID), eq(TEST_OTHER_USER_ID), eq(TEST_USER_ID));

            ResponseEntity<?> response = chatController.removeChatMember(TEST_CHAT_ID, TEST_OTHER_USER_ID, userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<?> apiResponse = extractApiResponse(response);
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Пользователь удален из чата");

            verify(chatService, times(1))
                    .removeChatMember(TEST_CHAT_ID, TEST_OTHER_USER_ID, TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("Обновление информации о чате")
    class ChatUpdateTests {

        @Test
        @DisplayName("Успешное обновление названия группы")
        void updateGroupName_Success() {
            UpdateGroupNameRequest request = new UpdateGroupNameRequest();
            request.setGroupName("Новое название группы");

            doNothing().when(chatService)
                    .updateGroupName(eq(TEST_CHAT_ID), eq("Новое название группы"), eq(TEST_USER_ID));

            ResponseEntity<ApiResponse<String>> response =
                    chatController.updateGroupName(TEST_CHAT_ID, request, userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<String> apiResponse = response.getBody();
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Название группы успешно изменено");
            assertThat(apiResponse.getData()).isEqualTo("Новое название группы");

            verify(chatService, times(1))
                    .updateGroupName(TEST_CHAT_ID, "Новое название группы", TEST_USER_ID);
        }

        @Test
        @DisplayName("Успешное обновление аватара чата")
        void updateAvatar_Success() throws Exception {
            MultipartFile file = mock(MultipartFile.class);

            doNothing().when(chatService)
                    .updateChatAvatar(eq(TEST_CHAT_ID), eq(file), eq(TEST_USER_ID));

            ResponseEntity<?> response = chatController.updateAvatar(TEST_CHAT_ID, file, userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<?> apiResponse = extractApiResponse(response);
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Фото группы успешно изменено");

            verify(chatService, times(1))
                    .updateChatAvatar(TEST_CHAT_ID, file, TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("Получение списка чатов")
    class GetChatsTests {

        @Test
        @DisplayName("Успешное получение списка чатов с параметрами по умолчанию")
        void getUserChats_WithDefaultParameters_Success() {
            Page<ChatPreviewResponse> mockPage = createChatPreviewPage(0, 50);

            when(chatService.getUserChatsPreview(eq(TEST_USER_ID), eq(0), eq(50)))
                    .thenReturn(mockPage);

            ResponseEntity<ApiResponse<Page<ChatPreviewResponse>>> response =
                    chatController.getUserChats(userDetails, 50, 0);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<Page<ChatPreviewResponse>> apiResponse = response.getBody();
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Самые новые чаты успешно загружены");
            assertThat(apiResponse.getData().getContent()).hasSize(1);

            verify(chatService, times(1))
                    .getUserChatsPreview(TEST_USER_ID, 0, 50);
        }

        @Test
        @DisplayName("Получение пустого списка чатов")
        void getUserChats_EmptyList_Success() {
            Page<ChatPreviewResponse> emptyPage = Page.empty(PageRequest.of(0, 50));

            when(chatService.getUserChatsPreview(eq(TEST_USER_ID), anyInt(), anyInt()))
                    .thenReturn(emptyPage);

            ResponseEntity<ApiResponse<Page<ChatPreviewResponse>>> response =
                    chatController.getUserChats(userDetails, 50, 0);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<Page<ChatPreviewResponse>> apiResponse = response.getBody();
            assertThat(apiResponse.getData().isEmpty()).isTrue();
            assertThat(apiResponse.getData().getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Обработка исключений")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Попытка добавления уже существующего участника")
        void addChatMember_AlreadyMember_ThrowsException() {
            AddChatMemberRequestDto request = new AddChatMemberRequestDto();
            request.setUserId(TEST_OTHER_USER_ID);

            doThrow(new IllegalArgumentException("Пользователь уже является участником чата"))
                    .when(chatService).addChatMember(any(AddChatMemberRequestDto.class), eq(TEST_USERNAME));

            assertThatThrownBy(() -> chatController.addChatMember(TEST_CHAT_ID, request, userDetails))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Пользователь уже является участником чата");
        }

        @Test
        @DisplayName("Удаление участника без прав администратора")
        void removeChatMember_NoAdminRights_ThrowsException() {
            doThrow(new AccessDeniedException("Недостаточно прав для удаления участника"))
                    .when(chatService).removeChatMember(eq(TEST_CHAT_ID), eq(TEST_OTHER_USER_ID), eq(TEST_USER_ID));

            assertThatThrownBy(() -> chatController.removeChatMember(TEST_CHAT_ID, TEST_OTHER_USER_ID, userDetails))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Недостаточно прав для удаления участника");
        }

        @Test
        @DisplayName("Обновление названия несуществующего чата")
        void updateGroupName_ChatNotFound_ThrowsException() {
            UpdateGroupNameRequest request = new UpdateGroupNameRequest();
            request.setGroupName("Новое название");

            doThrow(new ChatNotFoundException("Чат не найден"))
                    .when(chatService).updateGroupName(eq(TEST_CHAT_ID), eq("Новое название"), eq(TEST_USER_ID));

            assertThatThrownBy(() -> chatController.updateGroupName(TEST_CHAT_ID, request, userDetails))
                    .isInstanceOf(ChatNotFoundException.class)
                    .hasMessageContaining("Чат не найден");
        }
    }

    @Nested
    @DisplayName("Пограничные случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Выход из чата")
        void leaveChat_Success() {
            doNothing().when(chatService)
                    .leaveChat(eq(TEST_CHAT_ID), eq(TEST_USER_ID));

            ResponseEntity<?> response = chatController.leaveChat(TEST_CHAT_ID, userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<?> apiResponse = extractApiResponse(response);
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Вы вышли из чата");

            verify(chatService, times(1))
                    .leaveChat(TEST_CHAT_ID, TEST_USER_ID);
        }

        @Test
        @DisplayName("Разжалование администратора")
        void demoteAdminToMember_Success() {
            doNothing().when(chatService)
                    .demoteAdminToMember(eq(TEST_CHAT_ID), eq(TEST_OTHER_USER_ID), eq(TEST_USER_ID));

            ResponseEntity<?> response = chatController.demoteAdminToMember(TEST_CHAT_ID, TEST_OTHER_USER_ID, userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<?> apiResponse = extractApiResponse(response);
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Администратор разжалован до обычного участника");

            verify(chatService, times(1))
                    .demoteAdminToMember(TEST_CHAT_ID, TEST_OTHER_USER_ID, TEST_USER_ID);
        }
    }
}