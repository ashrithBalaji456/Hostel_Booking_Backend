package com.accommodation.platform.common.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DateTimeUtils {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private DateTimeUtils() {}

    public static ZonedDateTime toIst(Instant instant) {
        return instant == null ? null : instant.atZone(IST_ZONE);
    }
}
