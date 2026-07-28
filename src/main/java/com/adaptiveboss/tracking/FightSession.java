package com.adaptiveboss.tracking;

import org.jetbrains.annotations.NotNull;

/**
 * Wraps a {@link CombatProfile} with session-level expiry tracking.
 *
 * <p>A session expires when no hits have been recorded for longer than
 * {@code sessionExpiryTicks}. Expired sessions are excluded from party
 * aggregation so stale data from a player who retreated long ago cannot
 * permanently skew the boss's archetype.
 */
public final class FightSession {

    private final CombatProfile profile;
    private int lastHitTick;

    /**
     * Creates a new session, anchored to the current server tick.
     *
     * @param startTick the server tick at session creation
     */
    public FightSession(final int startTick) {
        this.profile     = new CombatProfile();
        this.lastHitTick = startTick;
    }

    /**
     * Returns {@code true} when this session has been idle long enough to
     * be considered expired.
     *
     * @param currentTick        the current server tick
     * @param sessionExpiryTicks maximum ticks of inactivity before expiry
     * @return {@code true} if the session has expired
     */
    public boolean isExpired(final int currentTick, final int sessionExpiryTicks) {
        return (currentTick - lastHitTick) > sessionExpiryTicks;
    }

    /**
     * Records a hit and refreshes the session's last-hit timestamp.
     *
     * @param type         the damage type
     * @param damageAmount the actual damage dealt
     * @param distance     attacker-to-boss distance at the time of the hit
     * @param currentTick  the current server tick
     */
    public void recordHit(
            @NotNull final DamageType type,
            final double damageAmount,
            final double distance,
            final int currentTick) {
        profile.recordHit(type, damageAmount, distance, currentTick);
        lastHitTick = currentTick;
    }

    /**
     * Applies decay to the underlying profile.
     *
     * @param factor decay multiplier in (0, 1]
     */
    public void decay(final double factor) {
        profile.decay(factor);
    }

    /**
     * Returns the underlying {@link CombatProfile}.
     *
     * @return the profile for this session
     */
    @NotNull
    public CombatProfile getProfile() {
        return profile;
    }

    /** Returns the server tick of the most recent hit. */
    public int getLastHitTick() {
        return lastHitTick;
    }
}
