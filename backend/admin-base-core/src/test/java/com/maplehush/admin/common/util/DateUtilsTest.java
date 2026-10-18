package com.maplehush.admin.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DateUtilsTest {
    @Test
    void databaseWriteTimesUseWholeSeconds() {
        var clock = Clock.fixed(Instant.parse("2026-10-02T00:00:00.987Z"), DateUtils.ZONE);
        assertEquals(LocalDateTime.of(2026, 10, 2, 8, 0), DateUtils.now(clock));
        assertEquals(LocalDateTime.of(2026, 10, 2, 8, 15), DateUtils.after(clock, Duration.ofMinutes(15)));
    }

    @Test
    void convertsOffsetFilterToBeijingDatabaseTime() {
        assertEquals(LocalDateTime.of(2026, 10, 2, 8, 30), DateUtils.fromIso("2026-10-02T08:30:00+08:00"));
    }
}
