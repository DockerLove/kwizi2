package com.example.kwizi.unit;
import com.example.kwizi.util.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class TimeUnitTest {

    @Test
    void millisecondsConversions() {
        assertEquals(1, TimeUnit.MILLISECONDS.toMillis(1));
        assertEquals(0, TimeUnit.MILLISECONDS.toSeconds(999));
        assertEquals(0, TimeUnit.MILLISECONDS.toMinutes(59_999));
        assertEquals(0, TimeUnit.MILLISECONDS.toHours(3_599_999));
        assertEquals(0, TimeUnit.MILLISECONDS.toDays(86_399_999));

        assertEquals(1, TimeUnit.MILLISECONDS.toSeconds(1000));
        assertEquals(1, TimeUnit.MILLISECONDS.toMinutes(60_000));
        assertEquals(1, TimeUnit.MILLISECONDS.toHours(3_600_000));
        assertEquals(1, TimeUnit.MILLISECONDS.toDays(86_400_000));
    }

    @Test
    void secondsConversions() {
        assertEquals(1000, TimeUnit.SECONDS.toMillis(1));
        assertEquals(1, TimeUnit.SECONDS.toSeconds(1));
        assertEquals(0, TimeUnit.SECONDS.toMinutes(59));
        assertEquals(0, TimeUnit.SECONDS.toHours(3599));
        assertEquals(0, TimeUnit.SECONDS.toDays(86_399));

        assertEquals(1, TimeUnit.SECONDS.toMinutes(60));
        assertEquals(1, TimeUnit.SECONDS.toHours(3600));
        assertEquals(1, TimeUnit.SECONDS.toDays(86_400));
    }

    @Test
    void minutesConversions() {
        assertEquals(60_000, TimeUnit.MINUTES.toMillis(1));
        assertEquals(60, TimeUnit.MINUTES.toSeconds(1));
        assertEquals(1, TimeUnit.MINUTES.toMinutes(1));
        assertEquals(0, TimeUnit.MINUTES.toHours(59));
        assertEquals(0, TimeUnit.MINUTES.toDays(1439));

        assertEquals(1, TimeUnit.MINUTES.toHours(60));
        assertEquals(1, TimeUnit.MINUTES.toDays(1440));
    }

    @Test
    void hoursConversions() {
        assertEquals(3_600_000, TimeUnit.HOURS.toMillis(1));
        assertEquals(3600, TimeUnit.HOURS.toSeconds(1));
        assertEquals(60, TimeUnit.HOURS.toMinutes(1));
        assertEquals(1, TimeUnit.HOURS.toHours(1));
        assertEquals(0, TimeUnit.HOURS.toDays(23));

        assertEquals(1, TimeUnit.HOURS.toDays(24));
    }

    @Test
    void daysConversions() {
        assertEquals(86_400_000, TimeUnit.DAYS.toMillis(1));
        assertEquals(86_400, TimeUnit.DAYS.toSeconds(1));
        assertEquals(1440, TimeUnit.DAYS.toMinutes(1));
        assertEquals(24, TimeUnit.DAYS.toHours(1));
        assertEquals(1, TimeUnit.DAYS.toDays(1));
    }

    @ParameterizedTest
    @EnumSource(TimeUnit.class)
    void zeroDuration(TimeUnit unit) {
        assertEquals(0, unit.toMillis(0));
        assertEquals(0, unit.toSeconds(0));
        assertEquals(0, unit.toMinutes(0));
        assertEquals(0, unit.toHours(0));
        assertEquals(0, unit.toDays(0));
    }

    @Test
    void boundaryValues() {
        // Проверка MILLISECONDS (нет умножения)
        assertEquals(Long.MAX_VALUE, TimeUnit.MILLISECONDS.toMillis(Long.MAX_VALUE));

        // Проверка SECONDS (умножение на 1000)
        long maxSecondsWithoutOverflow = Long.MAX_VALUE / 1000;
        assertEquals(maxSecondsWithoutOverflow * 1000, TimeUnit.SECONDS.toMillis(maxSecondsWithoutOverflow));

        // Проверка MINUTES (умножение на 60*1000)
        long maxMinutesWithoutOverflow = Long.MAX_VALUE / (60 * 1000L);
        assertEquals(maxMinutesWithoutOverflow * 60 * 1000, TimeUnit.MINUTES.toMillis(maxMinutesWithoutOverflow));
    }
}