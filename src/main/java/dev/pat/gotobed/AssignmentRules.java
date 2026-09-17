package dev.pat.gotobed;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class AssignmentRules {

    private AssignmentRules() {
    }

    static boolean shouldActivate(Assignment assignment, Instant now) {
        return assignment.status() == AssignmentStatus.SCHEDULED
                && !assignment.scheduledAt().isAfter(now);
    }

    static boolean isActive(Assignment assignment, Instant now) {
        return assignment.status() == AssignmentStatus.ACTIVE || shouldActivate(assignment, now);
    }

    static boolean shouldClearOffline(Assignment assignment, Instant now, Duration timeout) {
        Instant logoutAt = assignment.logoutAt();
        return assignment.status() == AssignmentStatus.ACTIVE
                && logoutAt != null
                && !logoutAt.plus(timeout).isAfter(now);
    }

    static long offlineMinutes(Assignment assignment, Instant now) {
        Instant logoutAt = assignment.logoutAt();
        if (logoutAt == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(logoutAt, now).toMinutes());
    }

    static Assignment onQuit(Assignment assignment, Instant now) {
        if (assignment.status() != AssignmentStatus.ACTIVE) {
            return assignment;
        }
        return assignment.withLogout(now);
    }

    static Assignment onJoinResume(Assignment assignment) {
        return assignment.withoutLogout().withName(assignment.name());
    }

    static Optional<Assignment> putReplacing(Map<UUID, Assignment> assignments, Assignment next) {
        return Optional.ofNullable(assignments.put(next.uuid(), next));
    }

    static List<Assignment> restore(List<Assignment> loaded, Instant now, Duration timeout) {
        List<Assignment> kept = new ArrayList<>();
        for (Assignment assignment : loaded) {
            if (shouldClearOffline(assignment, now, timeout)) {
                continue;
            }
            if (shouldActivate(assignment, now)) {
                kept.add(assignment.activate());
            } else {
                kept.add(assignment);
            }
        }
        return kept;
    }

    static Map<UUID, Assignment> index(List<Assignment> assignments) {
        Map<UUID, Assignment> map = new LinkedHashMap<>();
        for (Assignment assignment : assignments) {
            map.put(assignment.uuid(), assignment);
        }
        return map;
    }
}
