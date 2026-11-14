package com.portfolio.gateway.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class MutableClock extends Clock {

    private volatile Instant instant;

    public MutableClock() {
        this(Instant.parse("2026-01-01T00:00:00Z"));
    }

    public MutableClock(Instant start) {
        this.instant = start;
    }

    public void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("MutableClock is always UTC");
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
