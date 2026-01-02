package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
@DisplayName("JwtExceptionHandler тесты")
@ExtendWith(MockitoExtension.class)
class JwtExceptionHandlerTest {
    private JwtExceptionHandler jwtExceptionHandler;

    @BeforeEach
    void setUp() {
        jwtExceptionHandler = new JwtExceptionHandler();
    }

    @Nested
    @DisplayName("Успешное выполнение операций")
    class SuccessScenarios {

        @Test
        @DisplayName("Возвращает значение при успешной операции")
        void handleJwtOperation_ShouldReturnValue_WhenOperationSucceeds() {
            String operationName = "извлечение данных из токена";
            String expectedValue = "success";
            JwtExceptionHandler.JwtOperation<String> successfulOperation = () -> expectedValue;

            String result = jwtExceptionHandler.handleJwtOperation(successfulOperation, operationName);

            assertThat(result).isEqualTo(expectedValue);
        }

        @Test
        @DisplayName("Поддерживает различные типы возвращаемых значений")
        void handleJwtOperation_ShouldWorkWithDifferentReturnTypes() {
            String operationName = "операция с разными типами";

            Integer intResult = jwtExceptionHandler.handleJwtOperation(() -> 42, operationName);
            Boolean boolResult = jwtExceptionHandler.handleJwtOperation(() -> true, operationName);
            Object testObject = new Object();
            Object objectResult = jwtExceptionHandler.handleJwtOperation(() -> testObject, operationName);

            assertThat(intResult).isEqualTo(42);
            assertThat(boolResult).isTrue();
            assertThat(objectResult).isSameAs(testObject);
        }
    }

    @Nested
    @DisplayName("Обработка специфичных исключений JWT")
    class SpecificJwtExceptionHandling {

        @Test
        @DisplayName("Преобразует ExpiredJwtException в JwtAuthenticationException")
        void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenExpiredJwtExceptionOccurs() {
            String operationName = "проверка токена";
            String expectedMessage = "Срок действия JWT токена истек при " + operationName;
            ExpiredJwtException expiredException = mock(ExpiredJwtException.class);

            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtExceptionHandler.handleJwtOperation(() -> {
                        throw expiredException;
                    }, operationName)
            );

            assertThat(exception.getMessage()).isEqualTo(expectedMessage);
            assertThat(exception.getCause()).isSameAs(expiredException);
        }

        @Test
        @DisplayName("Преобразует MalformedJwtException в JwtAuthenticationException")
        void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenMalformedJwtExceptionOccurs() {
            String operationName = "парсинг токена";
            String expectedMessage = "JWT токен имеет неверный формат при " + operationName;
            MalformedJwtException malformedException = mock(MalformedJwtException.class);

            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtExceptionHandler.handleJwtOperation(() -> {
                        throw malformedException;
                    }, operationName)
            );

            assertThat(exception.getMessage()).isEqualTo(expectedMessage);
            assertThat(exception.getCause()).isSameAs(malformedException);
        }

        @Test
        @DisplayName("Преобразует SignatureException в JwtAuthenticationException")
        void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenSignatureExceptionOccurs() {
            String operationName = "верификация подписи";
            String expectedMessage = "Неверная подпись JWT токена при " + operationName;
            SignatureException signatureException = mock(SignatureException.class);

            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtExceptionHandler.handleJwtOperation(() -> {
                        throw signatureException;
                    }, operationName)
            );

            assertThat(exception.getMessage()).isEqualTo(expectedMessage);
            assertThat(exception.getCause()).isSameAs(signatureException);
        }

        @Test
        @DisplayName("Преобразует UnsupportedJwtException в JwtAuthenticationException")
        void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenUnsupportedJwtExceptionOccurs() {
            String operationName = "обработка токена";
            String expectedMessage = "Неподдерживаемый JWT токен при " + operationName;
            UnsupportedJwtException unsupportedException = mock(UnsupportedJwtException.class);

            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtExceptionHandler.handleJwtOperation(() -> {
                        throw unsupportedException;
                    }, operationName)
            );

            assertThat(exception.getMessage()).isEqualTo(expectedMessage);
            assertThat(exception.getCause()).isSameAs(unsupportedException);
        }

        @Test
        @DisplayName("Преобразует IllegalArgumentException в JwtAuthenticationException")
        void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenIllegalArgumentExceptionOccurs() {
            String operationName = "валидация токена";
            String expectedMessage = "JWT токен недействителен при " + operationName;
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Invalid token");

            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtExceptionHandler.handleJwtOperation(() -> {
                        throw illegalArgumentException;
                    }, operationName)
            );

            assertThat(exception.getMessage()).isEqualTo(expectedMessage);
            assertThat(exception.getCause()).isSameAs(illegalArgumentException);
        }

        @Test
        @DisplayName("Преобразует общий JwtException в JwtAuthenticationException")
        void handleJwtOperation_ShouldThrowJwtAuthenticationException_WhenGenericJwtExceptionOccurs() {
            String operationName = "извлечение claims";
            String expectedMessage = "Ошибка обработки JWT токена при " + operationName;
            JwtException genericJwtException = mock(JwtException.class);

            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtExceptionHandler.handleJwtOperation(() -> {
                        throw genericJwtException;
                    }, operationName)
            );

            assertThat(exception.getMessage()).isEqualTo(expectedMessage);
            assertThat(exception.getCause()).isSameAs(genericJwtException);
        }
    }

    @Nested
    @DisplayName("Другие сценарии")
    class OtherScenarios {

        @Test
        @DisplayName("Пробрасывает исключения, не относящиеся к JWT")
        void handleJwtOperation_ShouldPropagateException_WhenNonJwtExceptionOccurs() {
            String operationName = "чтение токена";
            RuntimeException runtimeException = new RuntimeException("Some other error");

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> jwtExceptionHandler.handleJwtOperation(() -> {
                        throw runtimeException;
                    }, operationName)
            );

            assertThat(exception).isSameAs(runtimeException);
        }

        @Test
        @DisplayName("Включает имя операции в сообщение об ошибке")
        void handleJwtOperation_ShouldIncludeOperationNameInErrorMessage() {
            String operationName = "специальная операция проверки";
            String expectedMessage = "Срок действия JWT токена истек при " + operationName;
            ExpiredJwtException expiredException = mock(ExpiredJwtException.class);

            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtExceptionHandler.handleJwtOperation(() -> {
                        throw expiredException;
                    }, operationName)
            );

            assertThat(exception.getMessage()).isEqualTo(expectedMessage).contains(operationName);
        }

        @Test
        @DisplayName("Сохраняет цепочку исключений")
        void handleJwtOperation_ShouldPreserveExceptionChain() {
            String operationName = "тест цепочки исключений";
            String causeMessage = "Оригинальная ошибка в JWT";
            MalformedJwtException originalException = new MalformedJwtException(causeMessage);

            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtExceptionHandler.handleJwtOperation(() -> {
                        throw originalException;
                    }, operationName)
            );

            assertThat(exception.getCause()).isNotNull().isSameAs(originalException);
            assertThat(exception.getCause().getMessage()).isEqualTo(causeMessage);
        }
    }
}