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
    void rejectsInvalidInput() {
        assertEquals(Optional.empty(), TimeParser.parse(null, BERLIN, NOW));
        assertEquals(Optional.empty(), TimeParser.parse("", BERLIN, NOW));
        assertEquals(Optional.empty(), TimeParser.parse("22", BERLIN, NOW));
        assertEquals(Optional.empty(), TimeParser.parse("24:00", BERLIN, NOW));
        assertEquals(Optional.empty(), TimeParser.parse("22:60", BERLIN, NOW));
        assertEquals(Optional.empty(), TimeParser.parse("abc", BERLIN, NOW));
    }
}
