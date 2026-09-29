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
    static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final Pattern TIME = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
    private static final Pattern RELATIVE = Pattern.compile("^([0-9]+)([mh])$", Pattern.CASE_INSENSITIVE);
    private static final long MAX_RELATIVE_SECONDS = 365L * 24 * 60 * 60;

    record Result(LocalTime time, Instant scheduledAt, boolean immediate, String display) {
        Result(LocalTime time, Instant scheduledAt, boolean immediate) {
            this(time, scheduledAt, immediate, time.format(DISPLAY));
        }
    }

    private TimeParser() {}

    record ScheduleParts(String time, String player, String sentence) {}

    static Optional<ScheduleParts> splitSchedule(String rest) {
        if (rest == null) {
            return Optional.empty();
        }
        String trimmed = rest.trim();
        int timeEnd = tokenEnd(trimmed, 0);
        if (timeEnd <= 0 || timeEnd == trimmed.length()) {
            return Optional.empty();
        }
        String time = trimmed.substring(0, timeEnd);
        String afterTime = trimmed.substring(timeEnd).trim();
        int playerEnd = tokenEnd(afterTime, 0);
        if (playerEnd <= 0 || playerEnd == afterTime.length()) {
            return Optional.empty();
        }
        String player = afterTime.substring(0, playerEnd);
        String sentence = afterTime.substring(playerEnd).trim();
        if (player.isEmpty() || sentence.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ScheduleParts(time, player, sentence));
    }

    private static int tokenEnd(String s, int from) {
        int i = from;
        while (i < s.length() && !Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    static Optional<Result> parse(String raw, ZoneId zone, Instant now) {
        if (raw == null) {
            return Optional.empty();
        }
        var relative = RELATIVE.matcher(raw.trim());
        if (relative.matches()) {
            try {
                long amount = Long.parseLong(relative.group(1));
                long unitSeconds = relative.group(2).equalsIgnoreCase("h") ? 3600L : 60L;
                if (amount < 1 || amount > MAX_RELATIVE_SECONDS / unitSeconds) {
                    return Optional.empty();
                }
                Instant scheduledAt = now.plusSeconds(amount * unitSeconds);
                ZonedDateTime local = scheduledAt.atZone(zone);
                return Optional.of(new Result(local.toLocalTime(), scheduledAt, false, local.format(DATE_DISPLAY)));
            } catch (NumberFormatException | DateTimeException ignored) {
                return Optional.empty();
            }
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
