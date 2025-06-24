package com.example.kwizi.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.example.kwizi.util.TimeUnit;
@Target(ElementType.METHOD)  // Применяется только к методам
@Retention(RetentionPolicy.RUNTIME)  // Доступна в runtime
public @interface RateLimited {
    int value();          // Лимит запросов (например, 5)
    TimeUnit timeUnit();  // Единица времени (SECONDS, MINUTES и т.д.)
    int duration();       // Временное окно (например, 1 = 1 минута)
}