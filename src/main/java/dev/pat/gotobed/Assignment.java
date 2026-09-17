package dev.pat.gotobed;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

record Assignment(
        UUID uuid,
        String name,
        String sentence,
        Instant scheduledAt,
        AssignmentStatus status,
        @Nullable Instant logoutAt,
        Speaker nextSpeaker) {
    static Assignment create(UUID uuid, String name, String sentence, Instant scheduledAt, boolean immediate) {
        return new Assignment(
                uuid,
                name,
                sentence,
                scheduledAt,
                immediate ? AssignmentStatus.ACTIVE : AssignmentStatus.SCHEDULED,
                null,
                Speaker.PAPA);
    }

    Assignment activate() {
        return new Assignment(uuid, name, sentence, scheduledAt, AssignmentStatus.ACTIVE, null, nextSpeaker);
    }

    Assignment withName(String name) {
        return new Assignment(uuid, name, sentence, scheduledAt, status, logoutAt, nextSpeaker);
    }

    Assignment withLogout(Instant at) {
        return new Assignment(uuid, name, sentence, scheduledAt, status, at, nextSpeaker);
    }

    Assignment withoutLogout() {
        return new Assignment(uuid, name, sentence, scheduledAt, status, null, nextSpeaker);
    }

    Assignment spoken() {
        return new Assignment(uuid, name, sentence, scheduledAt, status, logoutAt, nextSpeaker.next());
    }
}
