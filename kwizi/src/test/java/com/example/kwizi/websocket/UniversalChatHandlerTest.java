package com.example.kwizi.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UniversalChatHandlerTest {

    @InjectMocks
    private UniversalChatHandler universalChatHandler;

    @BeforeEach
    void setUp() {
        // Инициализация activeSessions
        ReflectionTestUtils.setField(universalChatHandler, "activeSessions", new ConcurrentHashMap<>());
    }

    @Test
    void afterConnectionClosed_ShouldRemoveSession_WhenValidUserId() throws Exception {
        // Arrange
        long userId = 123L;
        WebSocketSession session = mock(WebSocketSession.class);
        URI uri = new URI("ws://localhost:8080/chat?id=" + userId);
        when(session.getUri()).thenReturn(uri);
        when(session.getId()).thenReturn("session-123");

        // Добавляем сессию вручную (без вызова afterConnectionEstablished)
        Map<Long, WebSocketSession> activeSessions = getActiveSessions();
        activeSessions.put(userId, session);

        // Act
        universalChatHandler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Assert
        assertTrue(activeSessions.isEmpty());
    }

    @Test
    void afterConnectionClosed_ShouldNotFail_WhenSessionWithoutUserId() throws Exception {
        // Arrange
        WebSocketSession session = mock(WebSocketSession.class);
        URI uri = new URI("ws://localhost:8080/chat"); // Без параметра id
        when(session.getUri()).thenReturn(uri);

        // Act & Assert (должен завершиться без исключений)
        assertDoesNotThrow(() ->
                universalChatHandler.afterConnectionClosed(session, CloseStatus.NORMAL)
        );
    }

    @Test
    void afterConnectionClosed_ShouldThrowNPE_WhenSessionIsNull() {
        assertThrows(NullPointerException.class, () ->
                universalChatHandler.afterConnectionClosed(null, CloseStatus.NORMAL)
        );
    }

    @Test
    void afterConnectionClosed_ShouldNotFail_WhenSessionNotInActiveSessions() throws Exception {
        // Arrange
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(new URI("ws://localhost:8080/chat?id=999"));
        when(session.getId()).thenReturn("non-existent-session");

        // Act & Assert
        assertDoesNotThrow(() ->
                universalChatHandler.afterConnectionClosed(session, CloseStatus.NORMAL)
        );
    }

    private Map<Long, WebSocketSession> getActiveSessions() {
        return (Map<Long, WebSocketSession>)
                ReflectionTestUtils.getField(universalChatHandler, "activeSessions");
    }
}