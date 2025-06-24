package com.example.kwizi.util;

import java.time.temporal.TemporalUnit;

public enum TimeUnit {
    MILLISECONDS(1),
    SECONDS(1000),
    MINUTES(60 * 1000),
    HOURS(60 * 60 * 1000),
    DAYS(24 * 60 * 60 * 1000);

    private final long milliseconds;

    TimeUnit(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public long toMillis(long duration) {
        return duration * milliseconds;
    }

    public long toSeconds(long duration) {
        return duration * milliseconds / 1000;
    }

    public long toMinutes(long duration) {
        return duration * milliseconds / (60 * 1000);
    }

    public long toHours(long duration) {
        return duration * milliseconds / (60 * 60 * 1000);
    }

    public long toDays(long duration) {
        return duration * milliseconds / (24 * 60 * 60 * 1000);
    }


}