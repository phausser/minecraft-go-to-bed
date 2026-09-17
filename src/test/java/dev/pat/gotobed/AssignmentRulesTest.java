package dev.pat.gotobed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssignmentRulesTest {

    private static final Instant NOW = Instant.parse("2026-09-17T19:30:00Z");
    private static final Duration TEN_MIN = Duration.ofMinutes(10);
    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void scheduledInTheFutureIsNotDue() {
        Assignment assignment = Assignment.create(ID, "Pat", "ins Bett", NOW.plusSeconds(600), false);
        assertFalse(AssignmentRules.shouldActivate(assignment, NOW));
        assertFalse(AssignmentRules.isActive(assignment, NOW));
    }

    @Test
    void scheduledInThePastActivates() {
        Assignment assignment = Assignment.create(ID, "Pat", "ins Bett", NOW.minusSeconds(1), false);
        assertTrue(AssignmentRules.shouldActivate(assignment, NOW));
        assertTrue(AssignmentRules.isActive(assignment, NOW));
        assertEquals(AssignmentStatus.ACTIVE, assignment.activate().status());
        assertNull(assignment.activate().logoutAt());
    }

    @Test
    void nowCreatesActiveAssignment() {
        Assignment assignment = Assignment.create(ID, "Pat", "ins Bett", NOW, true);
        assertEquals(AssignmentStatus.ACTIVE, assignment.status());
        assertFalse(AssignmentRules.shouldActivate(assignment, NOW));
        assertTrue(AssignmentRules.isActive(assignment, NOW));
    }

    @Test
    void offlineTimerClearsAtTenMinutes() {
        Assignment assignment =
                Assignment.create(ID, "Pat", "ins Bett", NOW, true).withLogout(NOW.minus(Duration.ofMinutes(10)));
        assertTrue(AssignmentRules.shouldClearOffline(assignment, NOW, TEN_MIN));
        assertEquals(10L, AssignmentRules.offlineMinutes(assignment, NOW));
    }

    @Test
    void offlineTimerKeepsAtNineMinutes() {
        Assignment assignment =
                Assignment.create(ID, "Pat", "ins Bett", NOW, true).withLogout(NOW.minus(Duration.ofMinutes(9)));
        assertFalse(AssignmentRules.shouldClearOffline(assignment, NOW, TEN_MIN));
        assertEquals(9L, AssignmentRules.offlineMinutes(assignment, NOW));
    }

    @Test
    void quitOnlyStampsActiveAssignments() {
        Assignment scheduled = Assignment.create(ID, "Pat", "ins Bett", NOW.plusSeconds(60), false);
        assertNull(AssignmentRules.onQuit(scheduled, NOW).logoutAt());

        Assignment active = Assignment.create(ID, "Pat", "ins Bett", NOW, true);
        Assignment afterQuit = AssignmentRules.onQuit(active, NOW);
        assertEquals(NOW, afterQuit.logoutAt());
        assertEquals(AssignmentStatus.ACTIVE, afterQuit.status());
    }

    @Test
    void rejoinBeforeTimeoutClearsLogoutStamp() {
        Assignment offline =
                Assignment.create(ID, "Pat", "ins Bett", NOW, true).withLogout(NOW.minus(Duration.ofMinutes(2)));
        Assignment resumed = AssignmentRules.onJoinResume(offline);
        assertNull(resumed.logoutAt());
        assertEquals(AssignmentStatus.ACTIVE, resumed.status());
        assertFalse(AssignmentRules.shouldClearOffline(resumed, NOW, TEN_MIN));
    }

    @Test
    void restoreActivatesDueAndDropsExpiredOffline() {
        Assignment future = Assignment.create(ID, "Future", "warten", NOW.plusSeconds(120), false);
        Assignment due = Assignment.create(
                UUID.fromString("22222222-2222-2222-2222-222222222222"), "Due", "jetzt", NOW.minusSeconds(5), false);
        Assignment expired = Assignment.create(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        "Gone",
                        "weg",
                        NOW.minusSeconds(60),
                        true)
                .withLogout(NOW.minus(Duration.ofMinutes(11)));

        List<Assignment> restored = AssignmentRules.restore(List.of(future, due, expired), NOW, TEN_MIN);
        assertEquals(2, restored.size());
        assertEquals(AssignmentStatus.SCHEDULED, restored.get(0).status());
        assertEquals(AssignmentStatus.ACTIVE, restored.get(1).status());
        assertEquals("Due", restored.get(1).name());
    }

    @Test
    void secondAssignReplacesThePrevious() {
        Map<UUID, Assignment> map = new LinkedHashMap<>();
        Assignment first = Assignment.create(ID, "Pat", "alt", NOW.plusSeconds(60), false);
        Assignment second = Assignment.create(ID, "Pat", "neu", NOW, true);

        assertEquals(Optional.empty(), AssignmentRules.putReplacing(map, first));
        Optional<Assignment> previous = AssignmentRules.putReplacing(map, second);
        assertTrue(previous.isPresent());
        assertEquals("alt", previous.get().sentence());
        assertEquals("neu", map.get(ID).sentence());
        assertEquals(AssignmentStatus.ACTIVE, map.get(ID).status());
        assertEquals(1, map.size());
    }

    @Test
    void speakersAlternate() {
        Assignment assignment = Assignment.create(ID, "Pat", "ins Bett", NOW, true);
        assertEquals(Speaker.PAPA, assignment.nextSpeaker());
        assertEquals(Speaker.MAMA, assignment.spoken().nextSpeaker());
        assertEquals(Speaker.PAPA, assignment.spoken().spoken().nextSpeaker());
    }
}
