package com.example.kwizi.unit;

import com.example.kwizi.util.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;


@DisplayName("TimeUnit тесты")
class TimeUnitTest {

    @Nested
    @DisplayName("Конвертации из MILLISECONDS")
    class MillisecondsConversionsTest {

        @Test
        @DisplayName("Должен корректно конвертировать миллисекунды в другие единицы")
        void shouldConvertMillisecondsToOtherUnits() {
            assertAll("Milliseconds conversions",
                    () -> assertThat(TimeUnit.MILLISECONDS.toMillis(1L)).isOne(),
                    () -> assertThat(TimeUnit.MILLISECONDS.toSeconds(999L)).isZero(),
                    () -> assertThat(TimeUnit.MILLISECONDS.toMinutes(59_999L)).isZero(),
                    () -> assertThat(TimeUnit.MILLISECONDS.toHours(3_599_999L)).isZero(),
                    () -> assertThat(TimeUnit.MILLISECONDS.toDays(86_399_999L)).isZero()
            );
        }

        @Test
        @DisplayName("Должен корректно конвертировать граничные значения миллисекунд")
        void shouldConvertMillisecondsBoundaries() {
            assertAll("Milliseconds boundary conversions",
                    () -> assertThat(TimeUnit.MILLISECONDS.toSeconds(1_000L)).isOne(),
                    () -> assertThat(TimeUnit.MILLISECONDS.toMinutes(60_000L)).isOne(),
                    () -> assertThat(TimeUnit.MILLISECONDS.toHours(3_600_000L)).isOne(),
                    () -> assertThat(TimeUnit.MILLISECONDS.toDays(86_400_000L)).isOne()
            );
        }
    }

    @Nested
    @DisplayName("Конвертации из SECONDS")
    class SecondsConversionsTest {

        @Test
        @DisplayName("Должен корректно конвертировать секунды в другие единицы")
        void shouldConvertSecondsToOtherUnits() {
            assertAll("Seconds conversions",
                    () -> assertThat(TimeUnit.SECONDS.toMillis(1L)).isEqualTo(1_000L),
                    () -> assertThat(TimeUnit.SECONDS.toSeconds(1L)).isOne(),
                    () -> assertThat(TimeUnit.SECONDS.toMinutes(59L)).isZero(),
                    () -> assertThat(TimeUnit.SECONDS.toHours(3_599L)).isZero(),
                    () -> assertThat(TimeUnit.SECONDS.toDays(86_399L)).isZero()
            );
        }

        @Test
        @DisplayName("Должен корректно конвертировать граничные значения секунд")
        void shouldConvertSecondsBoundaries() {
            assertAll("Seconds boundary conversions",
                    () -> assertThat(TimeUnit.SECONDS.toMinutes(60L)).isOne(),
                    () -> assertThat(TimeUnit.SECONDS.toHours(3_600L)).isOne(),
                    () -> assertThat(TimeUnit.SECONDS.toDays(86_400L)).isOne()
            );
        }
    }

    @Nested
    @DisplayName("Конвертации из MINUTES")
    class MinutesConversionsTest {

        @Test
        @DisplayName("Должен корректно конвертировать минуты в другие единицы")
        void shouldConvertMinutesToOtherUnits() {
            assertAll("Minutes conversions",
                    () -> assertThat(TimeUnit.MINUTES.toMillis(1L)).isEqualTo(60_000L),
                    () -> assertThat(TimeUnit.MINUTES.toSeconds(1L)).isEqualTo(60L),
                    () -> assertThat(TimeUnit.MINUTES.toMinutes(1L)).isOne(),
                    () -> assertThat(TimeUnit.MINUTES.toHours(59L)).isZero(),
                    () -> assertThat(TimeUnit.MINUTES.toDays(1_439L)).isZero()
            );
        }

        @Test
        @DisplayName("Должен корректно конвертировать граничные значения минут")
        void shouldConvertMinutesBoundaries() {
            assertAll("Minutes boundary conversions",
                    () -> assertThat(TimeUnit.MINUTES.toHours(60L)).isOne(),
                    () -> assertThat(TimeUnit.MINUTES.toDays(1_440L)).isOne()
            );
        }
    }

    @Nested
    @DisplayName("Конвертации из HOURS")
    class HoursConversionsTest {

        @Test
        @DisplayName("Должен корректно конвертировать часы в другие единицы")
        void shouldConvertHoursToOtherUnits() {
            assertAll("Hours conversions",
                    () -> assertThat(TimeUnit.HOURS.toMillis(1L)).isEqualTo(3_600_000L),
                    () -> assertThat(TimeUnit.HOURS.toSeconds(1L)).isEqualTo(3_600L),
                    () -> assertThat(TimeUnit.HOURS.toMinutes(1L)).isEqualTo(60L),
                    () -> assertThat(TimeUnit.HOURS.toHours(1L)).isOne(),
                    () -> assertThat(TimeUnit.HOURS.toDays(23L)).isZero()
            );
        }

        @Test
        @DisplayName("Должен корректно конвертировать граничные значения часов")
        void shouldConvertHoursBoundaries() {
            assertThat(TimeUnit.HOURS.toDays(24L)).isOne();
        }
    }

    @Nested
    @DisplayName("Конвертации из DAYS")
    class DaysConversionsTest {

        @Test
        @DisplayName("Должен корректно конвертировать дни в другие единицы")
        void shouldConvertDaysToOtherUnits() {
            assertAll("Days conversions",
                    () -> assertThat(TimeUnit.DAYS.toMillis(1L)).isEqualTo(86_400_000L),
                    () -> assertThat(TimeUnit.DAYS.toSeconds(1L)).isEqualTo(86_400L),
                    () -> assertThat(TimeUnit.DAYS.toMinutes(1L)).isEqualTo(1_440L),
                    () -> assertThat(TimeUnit.DAYS.toHours(1L)).isEqualTo(24L),
                    () -> assertThat(TimeUnit.DAYS.toDays(1L)).isOne()
            );
        }
    }

    @Nested
    @DisplayName("Специальные случаи")
    class SpecialCasesTest {

        @ParameterizedTest
        @EnumSource(TimeUnit.class)
        @DisplayName("Должен возвращать 0 для нулевой длительности во всех единицах")
        void shouldReturnZeroForZeroDuration(TimeUnit unit) {
            assertAll("Zero duration for " + unit,
                    () -> assertThat(unit.toMillis(0L)).isZero(),
                    () -> assertThat(unit.toSeconds(0L)).isZero(),
                    () -> assertThat(unit.toMinutes(0L)).isZero(),
                    () -> assertThat(unit.toHours(0L)).isZero(),
                    () -> assertThat(unit.toDays(0L)).isZero()
            );
        }

        @Test
        @DisplayName("Должен корректно обрабатывать граничные значения без переполнения")
        void shouldHandleBoundaryValuesWithoutOverflow() {
            assertAll("Boundary values",
                    () -> assertThat(TimeUnit.MILLISECONDS.toMillis(Long.MAX_VALUE))
                            .isEqualTo(Long.MAX_VALUE),
                    () -> {
                        long maxSecondsWithoutOverflow = 9_223_372_036_854_775L;
                        assertThat(TimeUnit.SECONDS.toMillis(maxSecondsWithoutOverflow))
                                .isEqualTo(maxSecondsWithoutOverflow * 1_000L);
                    },
                    () -> {
                        long maxMinutesWithoutOverflow = 153_722_867_280_912L;
                        assertThat(TimeUnit.MINUTES.toMillis(maxMinutesWithoutOverflow))
                                .isEqualTo(maxMinutesWithoutOverflow * 60L * 1_000L);
                    }
            );
        }
    }

}