package dev.pat.gotobed;

import java.time.Duration;
import java.time.Instant;

final class DeadlineDelay {

    private static final Duration TICK = Duration.ofMillis(50);

    private DeadlineDelay() {}

    static long ticksUntil(Instant now, Instant deadline) {
        Duration remaining = Duration.between(now, deadline);
        if (remaining.isNegative() || remaining.isZero()) {
            return 1L;
        }
        long ticks = remaining.dividedBy(TICK);
        return Math.max(1L, remaining.minus(TICK.multipliedBy(ticks)).isZero() ? ticks : ticks + 1L);
    }
}
