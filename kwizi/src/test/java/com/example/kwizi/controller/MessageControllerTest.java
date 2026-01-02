package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.EditMessageRequest;
import com.example.kwizi.DTO.response.ApiResponseDto;
import com.example.kwizi.DTO.response.ChatHistoryResponse;
import com.example.kwizi.exception.MessageService.MessageNotFoundException;
import com.example.kwizi.service.ChatMessageService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("MessageController тесты")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageControllerTest {

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MessageController messageController;

    private static final String TEST_USERNAME = "testUser";
    private static final Long TEST_CHAT_ID = 1L;
    private static final Long TEST_MESSAGE_ID = 100L;
    private static final String TEST_MESSAGE_TEXT = "Updated message text";

    @BeforeEach
    void setUp() {
        when(authentication.getName()).thenReturn(TEST_USERNAME);
    }

    private ChatHistoryResponse createChatHistoryResponse() {
        ChatHistoryResponse response = new ChatHistoryResponse();
        response.setId(TEST_MESSAGE_ID);
        response.setText("Test message");
        response.setSenderId(1L);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    private Page<ChatHistoryResponse> createChatHistoryPage(int page, int size, int totalElements) {
        List<ChatHistoryResponse> messages = Collections.singletonList(createChatHistoryResponse());
        return new PageImpl<>(messages, PageRequest.of(page, size), totalElements);
    }

    @Nested
    @DisplayName("Получение истории чата")
    class GetChatHistoryTests {

        @Test
        @DisplayName("Успешное получение истории чата с параметрами по умолчанию")
        void getChatHistory_WithDefaultParameters_Success() {
            Page<ChatHistoryResponse> mockPage = createChatHistoryPage(0, 50, 1);

            when(chatMessageService.getChatHistory(
                    eq(TEST_CHAT_ID), eq(0), eq(50), eq("createdAt,desc"), eq(TEST_USERNAME)
            )).thenReturn(mockPage);

            var response = messageController.getChatHistory(
                    TEST_CHAT_ID, 0, 50, "createdAt,desc", authentication
            );

            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().isSuccess(),
                            r -> r.getBody().getMessage(),
                            r -> r.getBody().getData().getContent().size()
                    )
                    .containsExactly(200, true, "История чата успешно загружена", 1);

            verify(chatMessageService, times(1))
                    .getChatHistory(TEST_CHAT_ID, 0, 50, "createdAt,desc", TEST_USERNAME);
        }

        @Test
        @DisplayName("Успешное получение истории с кастомными параметрами пагинации")
        void getChatHistory_WithCustomParameters_Success() {
            int page = 2;
            int size = 25;
            String sort = "createdAt,asc";
            Page<ChatHistoryResponse> mockPage = createChatHistoryPage(page, size, 100);

            when(chatMessageService.getChatHistory(
                    eq(TEST_CHAT_ID), eq(page), eq(size), eq(sort), eq(TEST_USERNAME)
            )).thenReturn(mockPage);

            var response = messageController.getChatHistory(
                    TEST_CHAT_ID, page, size, sort, authentication
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getNumber()).isEqualTo(page);
            assertThat(response.getBody().getData().getSize()).isEqualTo(size);

            verify(chatMessageService, times(1))
                    .getChatHistory(TEST_CHAT_ID, page, size, sort, TEST_USERNAME);
        }

        @Test
        @DisplayName("Получение пустой истории чата")
        void getChatHistory_EmptyHistory_Success() {
            Page<ChatHistoryResponse> emptyPage = Page.empty(PageRequest.of(0, 50));

            when(chatMessageService.getChatHistory(
                    anyLong(), anyInt(), anyInt(), anyString(), anyString()
            )).thenReturn(emptyPage);

            var response = messageController.getChatHistory(
                    TEST_CHAT_ID, 0, 50, "createdAt,desc", authentication
            );

            assertThat(response.getBody().getData().isEmpty()).isTrue();
            assertThat(response.getBody().getData().getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Получение истории с сортировкой по разным полям")
        void getChatHistory_WithDifferentSorting_Success() {
            String[] sortParams = {"createdAt,asc", "senderUsername,desc", "id,asc"};

            for (String sort : sortParams) {
                Page<ChatHistoryResponse> mockPage = createChatHistoryPage(0, 50, 1);
                when(chatMessageService.getChatHistory(
                        eq(TEST_CHAT_ID), eq(0), eq(50), eq(sort), eq(TEST_USERNAME)
                )).thenReturn(mockPage);

                var response = messageController.getChatHistory(
                        TEST_CHAT_ID, 0, 50, sort, authentication
                );

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                reset(chatMessageService);
            }
        }
    }

    @Nested
    @DisplayName("Редактирование сообщений")
    class EditMessageTests {

        @Test
        @DisplayName("Успешное редактирование сообщения")
        void editMessage_Success() {
            EditMessageRequest request = new EditMessageRequest();
            request.setText(TEST_MESSAGE_TEXT);

            doNothing().when(chatMessageService)
                    .editMessage(eq(TEST_MESSAGE_ID), eq(TEST_MESSAGE_TEXT), eq(TEST_USERNAME));

            var response = messageController.editMessage(TEST_MESSAGE_ID, request, authentication);

            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> ((ApiResponseDto<?>) r.getBody()).isSuccess(),
                            r -> ((ApiResponseDto<?>) r.getBody()).getMessage()
                    )
                    .containsExactly(200, true, "Сообщение изменено");

            verify(chatMessageService, times(1))
                    .editMessage(TEST_MESSAGE_ID, TEST_MESSAGE_TEXT, TEST_USERNAME);
        }

        @Test
        @DisplayName("Редактирование с минимальным текстом")
        void editMessage_MinimalText_Success() {
            EditMessageRequest request = new EditMessageRequest();
            request.setText("a");

            doNothing().when(chatMessageService)
                    .editMessage(eq(TEST_MESSAGE_ID), eq("a"), eq(TEST_USERNAME));

            var response = messageController.editMessage(TEST_MESSAGE_ID, request, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Редактирование с максимальным текстом")
        void editMessage_LongText_Success() {
            String longText = "A".repeat(2000);
            EditMessageRequest request = new EditMessageRequest();
            request.setText(longText);

            doNothing().when(chatMessageService)
                    .editMessage(eq(TEST_MESSAGE_ID), eq(longText), eq(TEST_USERNAME));

            var response = messageController.editMessage(TEST_MESSAGE_ID, request, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("Удаление сообщений")
    class DeleteMessageTests {

        @Test
        @DisplayName("Успешное удаление сообщения")
        void deleteMessage_Success() {
            doNothing().when(chatMessageService)
                    .deleteMessage(eq(TEST_MESSAGE_ID), eq(TEST_USERNAME));

            var response = messageController.deleteMessage(TEST_MESSAGE_ID, authentication);

            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().isSuccess(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, true, "Сообщение успешно удалено");

            verify(chatMessageService, times(1))
                    .deleteMessage(TEST_MESSAGE_ID, TEST_USERNAME);
        }

        @Test
        @DisplayName("Удаление несуществующего сообщения (обработка сервисом)")
        void deleteMessage_NonExistentMessage_Success() {
            Long nonExistentMessageId = 999L;
            doNothing().when(chatMessageService)
                    .deleteMessage(eq(nonExistentMessageId), eq(TEST_USERNAME));

            var response = messageController.deleteMessage(nonExistentMessageId, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("Обработка исключений")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Сообщение не найдено при редактировании")
        void editMessage_NotFound() {
            EditMessageRequest request = new EditMessageRequest();
            request.setText(TEST_MESSAGE_TEXT);

            doThrow(new MessageNotFoundException("Сообщение не найдено"))
                    .when(chatMessageService)
                    .editMessage(eq(TEST_MESSAGE_ID), eq(TEST_MESSAGE_TEXT), eq(TEST_USERNAME));

            assertThatThrownBy(() ->
                    messageController.editMessage(TEST_MESSAGE_ID, request, authentication)
            )
                    .isInstanceOf(MessageNotFoundException.class)
                    .hasMessageContaining("Сообщение не найдено");
        }

        @Test
        @DisplayName("Нет прав для редактирования сообщения")
        void editMessage_Forbidden() {
            EditMessageRequest request = new EditMessageRequest();
            request.setText(TEST_MESSAGE_TEXT);

            doThrow(new AccessDeniedException("Нет прав для редактирования этого сообщения"))
                    .when(chatMessageService)
                    .editMessage(eq(TEST_MESSAGE_ID), eq(TEST_MESSAGE_TEXT), eq(TEST_USERNAME));

            assertThatThrownBy(() ->
                    messageController.editMessage(TEST_MESSAGE_ID, request, authentication)
            )
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Нет прав для редактирования");
        }

        @Test
        @DisplayName("Нет доступа к истории чата")
        void getChatHistory_AccessDenied() {
            when(chatMessageService.getChatHistory(
                    eq(TEST_CHAT_ID), anyInt(), anyInt(), anyString(), eq(TEST_USERNAME)
            )).thenThrow(new AccessDeniedException("Нет доступа к этому чату"));

            assertThatThrownBy(() ->
                    messageController.getChatHistory(TEST_CHAT_ID, 0, 50, "createdAt,desc", authentication)
            )
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Нет доступа к этому чату");
        }

        @Test
        @DisplayName("Сообщение уже удалено")
        void deleteMessage_AlreadyDeleted() {
            doThrow(new MessageNotFoundException("Сообщение уже удалено"))
                    .when(chatMessageService)
                    .deleteMessage(eq(TEST_MESSAGE_ID), eq(TEST_USERNAME));

            assertThatThrownBy(() ->
                    messageController.deleteMessage(TEST_MESSAGE_ID, authentication)
            )
                    .isInstanceOf(MessageNotFoundException.class)
                    .hasMessageContaining("Сообщение уже удалено");
        }

        @Test
        @DisplayName("Пустой текст при редактировании")
        void editMessage_EmptyText() {
            EditMessageRequest request = new EditMessageRequest();
            request.setText("");

            doThrow(new IllegalArgumentException("Текст сообщения не может быть пустым"))
                    .when(chatMessageService)
                    .editMessage(eq(TEST_MESSAGE_ID), eq(""), eq(TEST_USERNAME));

            assertThatThrownBy(() ->
                    messageController.editMessage(TEST_MESSAGE_ID, request, authentication)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Текст сообщения не может быть пустым");
        }

        @Test
        @DisplayName("Слишком длинный текст при редактировании")
        void editMessage_TextTooLong() {
            String tooLongText = "A".repeat(5001);
            EditMessageRequest request = new EditMessageRequest();
            request.setText(tooLongText);

            doThrow(new IllegalArgumentException("Текст сообщения слишком длинный"))
                    .when(chatMessageService)
                    .editMessage(eq(TEST_MESSAGE_ID), eq(tooLongText), eq(TEST_USERNAME));

            assertThatThrownBy(() ->
                    messageController.editMessage(TEST_MESSAGE_ID, request, authentication)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Текст сообщения слишком длинный");
        }
    }
}