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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Aspect
@Component // Обязательно, чтобы Spring распознал этот класс как аспект
public class RateLimiterAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterAspect.class);
    private final Map<String, Long> lastRequestTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();

    @Before("@annotation(com.example.kwizi.annotations.RateLimited)")
    public void rateLimit(JoinPoint joinPoint) throws RateLimitExceededException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimited rateLimited = method.getAnnotation(RateLimited.class);

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ipAddress = request.getRemoteAddr();
        String key = ipAddress + ":" + method.getName();

        int limit = rateLimited.value();
        TimeUnit timeUnit = rateLimited.timeUnit();
        long durationMillis = timeUnit.toMillis(rateLimited.duration());

        logger.debug("Rat limit check for method: {}, IP: {}, limit: {}, duration: {}ms", method.getName(), ipAddress, limit, durationMillis);

        long now = System.currentTimeMillis();
        long lastRequestTime = lastRequestTimestamps.getOrDefault(key, 0L);
        int requestCount = requestCounts.getOrDefault(key, 0);

        if (now - lastRequestTime > durationMillis) {
            logger.debug("Rate limit reset for IP: {}, method: {}", ipAddress, method.getName());
            requestCounts.put(key, 1);
            lastRequestTimestamps.put(key, now);
        } else {
            if (requestCount >= limit) {
                String errorMessage = "Rate limit exceeded for IP: " + ipAddress + ", method: " + method.getName();
                logger.warn(errorMessage);
                throw new RateLimitExceededException("Превышен лимит запросов. Пожалуйста, попробуйте позже.");
            }
            requestCounts.put(key, requestCount + 1);
            lastRequestTimestamps.put(key, now);
            logger.trace("Request count updated for IP: {}, method: {}, count: {}", ipAddress, method.getName(), requestCount + 1);
        }
    }
}