package dev.pat.gotobed;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Pattern;

final class TimeParser {

    static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern TIME = Pattern.compile("^(\\d{1,2}):(\\d{2})$");

    record Result(LocalTime time, Instant scheduledAt, boolean immediate) {
        String display() {
            return time.format(DISPLAY);
        }
    }

    private TimeParser() {}

    static Optional<Result> parse(String raw, ZoneId zone, Instant now) {
        if (raw == null) {
            return Optional.empty();
        }
        var matcher = TIME.matcher(raw.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        if (hour > 23 || minute > 59) {
            return Optional.empty();
        }
        try {
            LocalTime time = LocalTime.of(hour, minute);
            ZonedDateTime today = now.atZone(zone)
                    .withHour(time.getHour())
                    .withMinute(time.getMinute())
                    .withSecond(0)
                    .withNano(0);
            boolean immediate = !today.toInstant().isAfter(now);
            Instant scheduledAt = immediate ? now : today.toInstant();
            return Optional.of(new Result(time, scheduledAt, immediate));
        } catch (DateTimeException ignored) {
            return Optional.empty();
        }
    }
}
