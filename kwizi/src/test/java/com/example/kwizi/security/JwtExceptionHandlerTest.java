package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import io.jsonwebtoken.security.SignatureException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JwtExceptionHandlerTest {

    private JwtExceptionHandler jwtExceptionHandler;

    @BeforeEach
    void setUp() {
        jwtExceptionHandler = new JwtExceptionHandler();
    }

    // Тест для успешного выполнения операции
    @Test
    void handleJwtOperation_ShouldReturnValue_WhenOperationSucceeds() {
        // Arrange
        String operationName = "извлечение данных из токена";
        String expectedValue = "success";

        JwtExceptionHandler.JwtOperation<String> successfulOperation = () -> expectedValue;

        // Act
        String result = jwtExceptionHandler.handleJwtOperation(successfulOperation, operationName);

        // Assert
        assertEquals(expectedValue, result);
    }

    // Тест для ExpiredJwtException
    @Test
    void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenExpiredJwtExceptionOccurs() {
        // Arrange
        String operationName = "проверка токена";
        String expectedMessage = "Срок действия JWT токена истек при " + operationName;

        ExpiredJwtException expiredException = mock(ExpiredJwtException.class);
        JwtExceptionHandler.JwtOperation<String> failingOperation = () -> {
            throw expiredException;
        };

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtExceptionHandler.handleJwtOperation(failingOperation, operationName)
        );

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(expiredException, exception.getCause());
    }

    // Тест для MalformedJwtException
    @Test
    void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenMalformedJwtExceptionOccurs() {
        // Arrange
        String operationName = "парсинг токена";
        String expectedMessage = "JWT токен имеет неверный формат при " + operationName;

        MalformedJwtException malformedException = mock(MalformedJwtException.class);
        JwtExceptionHandler.JwtOperation<String> failingOperation = () -> {
            throw malformedException;
        };

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtExceptionHandler.handleJwtOperation(failingOperation, operationName)
        );

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(malformedException, exception.getCause());
    }

    // Тест для SignatureException
    @Test
    void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenSignatureExceptionOccurs() {
        // Arrange
        String operationName = "верификация подписи";
        String expectedMessage = "Неверная подпись JWT токена при " + operationName;

        SignatureException signatureException = mock(SignatureException.class);
        JwtExceptionHandler.JwtOperation<String> failingOperation = () -> {
            throw signatureException;
        };

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtExceptionHandler.handleJwtOperation(failingOperation, operationName)
        );

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(signatureException, exception.getCause());
    }

    // Тест для UnsupportedJwtException
    @Test
    void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenUnsupportedJwtExceptionOccurs() {
        // Arrange
        String operationName = "обработка токена";
        String expectedMessage = "Неподдерживаемый JWT токен при " + operationName;

        UnsupportedJwtException unsupportedException = mock(UnsupportedJwtException.class);
        JwtExceptionHandler.JwtOperation<String> failingOperation = () -> {
            throw unsupportedException;
        };

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtExceptionHandler.handleJwtOperation(failingOperation, operationName)
        );

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(unsupportedException, exception.getCause());
    }

    // Тест для IllegalArgumentException
    @Test
    void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenIllegalArgumentExceptionOccurs() {
        // Arrange
        String operationName = "валидация токена";
        String expectedMessage = "JWT токен недействителен при " + operationName;

        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Invalid token");
        JwtExceptionHandler.JwtOperation<String> failingOperation = () -> {
            throw illegalArgumentException;
        };

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtExceptionHandler.handleJwtOperation(failingOperation, operationName)
        );

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(illegalArgumentException, exception.getCause());
    }

    // Тест для общего JwtException (не конкретного подтипа)
    @Test
    void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenGenericJwtExceptionOccurs() {
        // Arrange
        String operationName = "извлечение claims";
        String expectedMessage = "Ошибка обработки JWT токена при " + operationName;

        JwtException genericJwtException = mock(JwtException.class);
        JwtExceptionHandler.JwtOperation<String> failingOperation = () -> {
            throw genericJwtException;
        };

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtExceptionHandler.handleJwtOperation(failingOperation, operationName)
        );

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(genericJwtException, exception.getCause());
    }

    // Тест для любого другого исключения (не JwtException)
    @Test
    void handleJwtOperation_ShouldPropagateException_WhenNonJwtExceptionOccurs() {
        // Arrange
        String operationName = "чтение токена";

        RuntimeException runtimeException = new RuntimeException("Some other error");
        JwtExceptionHandler.JwtOperation<String> failingOperation = () -> {
            throw runtimeException;
        };

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jwtExceptionHandler.handleJwtOperation(failingOperation, operationName)
        );

        assertEquals(runtimeException, exception);
    }

    // Тест для проверки что operationName включается в сообщение об ошибке
    @Test
    void handleJwtOperation_ShouldIncludeOperationNameInErrorMessage() {
        // Arrange
        String operationName = "специальная операция проверки";
        String expectedMessage = "Срок действия JWT токена истек при " + operationName;

        ExpiredJwtException expiredException = mock(ExpiredJwtException.class);
        JwtExceptionHandler.JwtOperation<String> failingOperation = () -> {
            throw expiredException;
        };

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtExceptionHandler.handleJwtOperation(failingOperation, operationName)
        );

        assertTrue(exception.getMessage().contains(operationName));
        assertEquals(expectedMessage, exception.getMessage());
    }

    // Тест для операции, возвращающей различные типы данных
    @Test
    void handleJwtOperation_ShouldWorkWithDifferentReturnTypes() {
        // Arrange
        String operationName = "операция с разными типами";

        // Тест с Integer
        JwtExceptionHandler.JwtOperation<Integer> intOperation = () -> 42;
        Integer intResult = jwtExceptionHandler.handleJwtOperation(intOperation, operationName);
        assertEquals(42, intResult);

        // Тест с Boolean
        JwtExceptionHandler.JwtOperation<Boolean> boolOperation = () -> true;
        Boolean boolResult = jwtExceptionHandler.handleJwtOperation(boolOperation, operationName);
        assertTrue(boolResult);

        // Тест с Object
        Object testObject = new Object();
        JwtExceptionHandler.JwtOperation<Object> objectOperation = () -> testObject;
        Object objectResult = jwtExceptionHandler.handleJwtOperation(objectOperation, operationName);
        assertEquals(testObject, objectResult);
    }

    // Тест для проверки цепочки исключений
    @Test
    void handleJwtOperation_ShouldPreserveExceptionChain() {
        // Arrange
        String operationName = "тест цепочки исключений";
        String causeMessage = "Оригинальная ошибка в JWT";

        MalformedJwtException originalException = new MalformedJwtException(causeMessage);
        JwtExceptionHandler.JwtOperation<String> failingOperation = () -> {
            throw originalException;
        };

        // Act
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtExceptionHandler.handleJwtOperation(failingOperation, operationName)
        );

        // Assert
        assertNotNull(exception.getCause());
        assertEquals(originalException, exception.getCause());
        assertEquals(causeMessage, exception.getCause().getMessage());
    }
}