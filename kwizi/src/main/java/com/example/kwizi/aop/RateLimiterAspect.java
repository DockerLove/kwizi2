package com.example.kwizi.aop;
import com.example.kwizi.annotations.RateLimited;
import com.example.kwizi.exception.RateLimitExceededException;
import com.example.kwizi.util.TimeUnit;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Aspect
@Component
public class RateLimiterAspect {

    private final Map<String, Long> lastRequestTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();

    @Before("@annotation(com.example.kwizi.annotations.RateLimited)")
    public void rateLimit(JoinPoint joinPoint) throws RateLimitExceededException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimited rateLimited = method.getAnnotation(RateLimited.class);

        // Получаем HttpServletRequest
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String ipAddress = request.getRemoteAddr();  // Используем IP-адрес для идентификации пользователя

        // Формируем ключ для хранения информации о запросах
        String key = ipAddress + ":" + method.getName();

        int limit = rateLimited.value();
        TimeUnit timeUnit = rateLimited.timeUnit();
        long durationMillis = timeUnit.toMillis(rateLimited.duration());

        long now = System.currentTimeMillis();
        long lastRequestTime = lastRequestTimestamps.getOrDefault(key, 0L);
        int requestCount = requestCounts.getOrDefault(key, 0);

        if (now - lastRequestTime > durationMillis) {
            // Если прошло больше времени, чем задано в rate limit, сбрасываем счетчик
            requestCounts.put(key, 1);
            lastRequestTimestamps.put(key, now);
        } else {
            if (requestCount >= limit) {
                throw new RateLimitExceededException("Превышен лимит запросов. Пожалуйста, попробуйте позже.");
            }
            requestCounts.put(key, requestCount + 1);
            lastRequestTimestamps.put(key, now);
        }
    }
}