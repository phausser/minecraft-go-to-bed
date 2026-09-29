package dev.pat.gotobed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeadlineDelayTest {

    private static final Instant NOW = Instant.parse("2026-09-29T18:00:00Z");

    @Test
    void partialTicksRoundUpIncludingSubMillisecondRemainders() {
        assertEquals(1, DeadlineDelay.ticksUntil(NOW, NOW.plusNanos(1)));
        assertEquals(1, DeadlineDelay.ticksUntil(NOW, NOW.plusMillis(49)));
        assertEquals(2, DeadlineDelay.ticksUntil(NOW, NOW.plusMillis(50).plusNanos(1)));
        assertEquals(2, DeadlineDelay.ticksUntil(NOW, NOW.plusMillis(99)));
    }

    @Test
    void exactTicksDoNotAddAnExtraTick() {
        assertEquals(1, DeadlineDelay.ticksUntil(NOW, NOW.plusMillis(50)));
        assertEquals(2, DeadlineDelay.ticksUntil(NOW, NOW.plusMillis(100)));
        assertEquals(12000, DeadlineDelay.ticksUntil(NOW, NOW.plusSeconds(600)));
    }

    @Test
    void overdueDeadlinesRunOnTheNextTick() {
        assertEquals(1, DeadlineDelay.ticksUntil(NOW, NOW));
        assertEquals(1, DeadlineDelay.ticksUntil(NOW, NOW.minusSeconds(600)));
    }

    @Test
    void earlyOfflineCheckSchedulesTheRemainingTime() {
        Instant deadline = NOW.plusSeconds(600);
        Instant early = deadline.minusMillis(25);
        Assignment offline = Assignment.create(UUID.randomUUID(), "Pat", "ins Bett", NOW, true)
                .withLogout(NOW);
        assertFalse(AssignmentRules.shouldClearOffline(offline, early, Duration.ofMinutes(10)));
        long retryTicks = DeadlineDelay.ticksUntil(early, deadline);
        assertEquals(1, retryTicks);
        assertTrue(
                AssignmentRules.shouldClearOffline(offline, early.plusMillis(retryTicks * 50), Duration.ofMinutes(10)));
        assertEquals(12001, DeadlineDelay.ticksUntil(NOW.minusMillis(25), deadline));
    }
}
