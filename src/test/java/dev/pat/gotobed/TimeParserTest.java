package dev.pat.gotobed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TimeParserTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final Instant NOW =
            ZonedDateTime.of(2026, 9, 17, 21, 30, 0, 0, BERLIN).toInstant();

    @Test
    void parsesHourMinuteAndPaddedHour() {
        assertEquals(
                LocalTime.of(9, 0),
                TimeParser.parse("9:00", BERLIN, NOW).orElseThrow().time());
        assertEquals(
                LocalTime.of(9, 0),
                TimeParser.parse("09:00", BERLIN, NOW).orElseThrow().time());
        assertEquals(
                LocalTime.of(22, 0),
                TimeParser.parse("22:00", BERLIN, NOW).orElseThrow().time());
    }

    @Test
    void futureTodayWaits() {
        TimeParser.Result result = TimeParser.parse("22:00", BERLIN, NOW).orElseThrow();
        assertFalse(result.immediate());
        assertEquals(ZonedDateTime.of(2026, 9, 17, 22, 0, 0, 0, BERLIN).toInstant(), result.scheduledAt());
    }

    @Test
    void pastTodayStartsImmediately() {
        TimeParser.Result result = TimeParser.parse("21:00", BERLIN, NOW).orElseThrow();
        assertTrue(result.immediate());
        assertEquals(NOW, result.scheduledAt());
    }

    @Test
    void exactNowStartsImmediately() {
        TimeParser.Result result = TimeParser.parse("21:30", BERLIN, NOW).orElseThrow();
        assertTrue(result.immediate());
        assertEquals(NOW, result.scheduledAt());
    }

    @Test
    void splitsTimePlayerAndSentence() {
        var parts = TimeParser.splitSchedule("22:10 Z_o_o_m Ab ins Bett!").orElseThrow();
        assertEquals("22:10", parts.time());
        assertEquals("Z_o_o_m", parts.player());
        assertEquals("Ab ins Bett!", parts.sentence());
    }

    @Test
    void splitRejectsIncompleteSchedule() {
        assertEquals(Optional.empty(), TimeParser.splitSchedule("22:10"));
        assertEquals(Optional.empty(), TimeParser.splitSchedule("22:10 Z_o_o_m"));
        assertEquals(Optional.empty(), TimeParser.splitSchedule(""));
    }

    @Test
    void relativeMinutesPreserveSecondsAndHoursAcceptUppercase() {
        Instant start = NOW.plusSeconds(17).plusNanos(123);
        var minutes = TimeParser.parse("30m", BERLIN, start).orElseThrow();
        assertEquals(start.plusSeconds(1800), minutes.scheduledAt());
        assertFalse(minutes.immediate());
        assertEquals("17.09.2026 22:00:17", minutes.display());
        assertEquals(
                start.plusSeconds(7200),
                TimeParser.parse(" 2H ", BERLIN, start).orElseThrow().scheduledAt());
    }

    @Test
    void relativeTimeCanCrossMidnight() {
        var result = TimeParser.parse("3h", BERLIN, NOW).orElseThrow();
        assertEquals(NOW.plusSeconds(10800), result.scheduledAt());
        assertEquals("18.09.2026 00:30:00", result.display());
        assertFalse(result.immediate());
    }

    @Test
    void relativeHoursAreElapsedTimeAcrossBothClockChanges() {
        for (String timestamp : new String[] {"2026-03-29T00:30:00Z", "2026-10-25T00:30:00Z"}) {
            Instant start = Instant.parse(timestamp);
            assertEquals(
                    start.plusSeconds(7200),
                    TimeParser.parse("2h", BERLIN, start).orElseThrow().scheduledAt());
        }
    }

    @Test
    void relativeDurationHasPositiveBoundedWholeUnits() {
        for (String invalid : new String[] {
            "0m",
            "0h",
            "-1m",
            "+1h",
            "1.5h",
            "1h30m",
            "30 m",
            "1d",
            "60s",
            "525601m",
            "8761h",
            "999999999999999999999999999m"
        }) {
            assertTrue(TimeParser.parse(invalid, BERLIN, NOW).isEmpty(), invalid);
        }
        assertEquals(
                NOW.plusSeconds(365L * 86400),
                TimeParser.parse("8760h", BERLIN, NOW).orElseThrow().scheduledAt());
        assertEquals(
                NOW.plusSeconds(365L * 86400),
                TimeParser.parse("525600m", BERLIN, NOW).orElseThrow().scheduledAt());
        assertEquals(
                NOW.plusSeconds(60),
                TimeParser.parse("1m", BERLIN, NOW).orElseThrow().scheduledAt());
    }

    @Test
    void splitsRelativeTimeWithoutChangingTheSentence() {
        var parts = TimeParser.splitSchedule("30m Z_o_o_m Ab ins Bett!").orElseThrow();
        assertEquals("30m", parts.time());
        assertEquals("Z_o_o_m", parts.player());
        assertEquals("Ab ins Bett!", parts.sentence());
    }

    @Test
    void rejectsInvalidInput() {
        assertEquals(Optional.empty(), TimeParser.parse(null, BERLIN, NOW));
        assertEquals(Optional.empty(), TimeParser.parse("", BERLIN, NOW));
        assertEquals(Optional.empty(), TimeParser.parse("22", BERLIN, NOW));
        assertEquals(Optional.empty(), TimeParser.parse("24:00", BERLIN, NOW));
        assertEquals(Optional.empty(), TimeParser.parse("22:60", BERLIN, NOW));
        assertEquals(Optional.empty(), TimeParser.parse("abc", BERLIN, NOW));
    }
}
