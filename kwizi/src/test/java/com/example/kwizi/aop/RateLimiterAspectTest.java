package com.example.kwizi.aop;

import com.example.kwizi.annotations.RateLimited;
import com.example.kwizi.exception.RateLimitExceededException;
import com.example.kwizi.util.TimeUnit;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("RateLimiterAspect тесты")
@ExtendWith(MockitoExtension.class)
@Nested
class RateLimiterAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @InjectMocks
    private RateLimiterAspect rateLimiterAspect;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        Method method = RateLimiterAspectTest.class.getDeclaredMethod("testMethod");
        request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("Ограничение частоты запросов")
    class RateLimitingScenarios {

        @Test
        @DisplayName("Разрешает первый запрос")
        void rateLimit_ShouldAllowFirstRequest() {
            assertThatNoException().isThrownBy(() -> rateLimiterAspect.rateLimit(joinPoint));
        }

        @Test
        @DisplayName("Разрешает запрос после сброса временного окна")
        void rateLimit_ShouldAllowRequestWithinLimit() throws InterruptedException {
            assertThatNoException().isThrownBy(() -> rateLimiterAspect.rateLimit(joinPoint));
            Thread.sleep(2000);
            assertThatNoException().isThrownBy(() -> rateLimiterAspect.rateLimit(joinPoint));
        }

        @Test
        @DisplayName("Выбрасывает исключение при превышении лимита")
        void rateLimit_ShouldThrowRateLimitExceededException() {
            assertThatNoException().isThrownBy(() -> rateLimiterAspect.rateLimit(joinPoint));
            assertThatNoException().isThrownBy(() -> rateLimiterAspect.rateLimit(joinPoint));
            assertThatThrownBy(() -> rateLimiterAspect.rateLimit(joinPoint))
                    .isInstanceOf(RateLimitExceededException.class)
                    .withFailMessage("Превышен лимит запросов. Пожалуйста, попробуйте позже.");
        }
    }
    @RateLimited(value = 2, duration = 1, timeUnit = TimeUnit.SECONDS)
    private void testMethod() {}

}