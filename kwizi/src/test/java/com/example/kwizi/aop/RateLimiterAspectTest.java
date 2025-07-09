package com.example.kwizi.aop;
import com.example.kwizi.annotations.RateLimited;
import com.example.kwizi.exception.RateLimitExceededException;
import com.example.kwizi.util.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RateLimiterAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @InjectMocks
    private RateLimiterAspect rateLimiterAspect;

    private Method method;

    private HttpServletRequest request;

    @BeforeEach
    public void setup() throws NoSuchMethodException {
        method = this.getClass().getDeclaredMethod("testMethod");
        request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).setRemoteAddr("127.0.0.1");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
    }

    @Test
    public void rateLimit_ShouldAllowFirstRequest() {
        // Arrange
        // Act & Assert
        assertDoesNotThrow(() -> rateLimiterAspect.rateLimit(joinPoint));
    }

    @Test
    public void rateLimit_ShouldAllowRequestWithinLimit() {
        // Arrange
        // Act
        assertDoesNotThrow(() -> rateLimiterAspect.rateLimit(joinPoint)); // First Request

        // Simulate enough time passing to reset the rate limiter
        try {
            Thread.sleep(2000); // Sleep for slightly more than the test duration so the counter resets
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert
        assertDoesNotThrow(() -> rateLimiterAspect.rateLimit(joinPoint)); // Second Request will be allowed
    }

    @Test
    public void rateLimit_ShouldThrowRateLimitExceededException() throws InterruptedException {
        // Arrange
        // Act & Assert
        assertDoesNotThrow(() -> rateLimiterAspect.rateLimit(joinPoint)); // First Request
        assertDoesNotThrow(() -> rateLimiterAspect.rateLimit(joinPoint)); // Second Request will be allowed

        //  Since we have value = 2, 3rd request must fail
        assertThrows(RateLimitExceededException.class, () -> rateLimiterAspect.rateLimit(joinPoint));
    }


    @RateLimited(value = 2, duration = 1, timeUnit = TimeUnit.SECONDS) // keep it 1 second so the test passes in reasonable time
    public void testMethod() {
        // Dummy method for testing the aspect
    }
}