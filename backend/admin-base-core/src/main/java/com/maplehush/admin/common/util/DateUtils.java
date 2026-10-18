package com.maplehush.admin.common.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 系统日期时间工具。数据库 DATETIME 和接口展示统一使用北京时间。
 */
public final class DateUtils {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private DateUtils() {}

    /** 获取北京时间，用于写入不带时区的 DATETIME 字段。 */
    public static LocalDateTime now(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), ZONE).truncatedTo(ChronoUnit.SECONDS);
    }

    /** 按真实经过的时长计算到期时间，再转换为北京时间。 */
    public static LocalDateTime after(Clock clock, Duration duration) {
        return LocalDateTime.ofInstant(clock.instant().plus(duration), ZONE).truncatedTo(ChronoUnit.SECONDS);
    }

    /** 将带时区的 ISO-8601 时间转换为数据库使用的北京时间。 */
    public static LocalDateTime fromIso(String value) {
        return LocalDateTime.ofInstant(Instant.parse(value), ZONE);
    }
}
