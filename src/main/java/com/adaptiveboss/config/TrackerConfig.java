package com.adaptiveboss.config;

/**
 * Read-only view of the configuration values consumed by
 * {@link com.adaptiveboss.tracking.BossCombatTracker}.
 *
 * <p>Implemented by {@link AdaptiveBossConfig} in production. Unit tests
 * supply a simple stub without bootstrapping the NeoForge
 * {@code ModConfigSpec} runtime.
 */
public interface TrackerConfig {

    /**
     * Whether hits are weighted by actual damage dealt.
     *
     * @return {@code true} if damage-based weighting is enabled
     */
    boolean isWeightHitsByDamage();

    /**
     * Ticks of combat inactivity after which a player's session expires.
     *
     * @return expiry threshold in ticks
     */
    int getSessionExpiryTicks();

    /**
     * Exponential decay factor applied to all combat weights each cycle.
     *
     * @return a value in (0, 1]
     */
    double getDecayFactor();

    /**
     * Melee damage ratio at or above which the boss adopts the KITER archetype.
     *
     * @return threshold in [0, 1]
     */
    double getMeleeThreshold();

    /**
     * Ranged damage ratio at or above which the boss adopts the AGGRESSOR archetype.
     *
     * @return threshold in [0, 1]
     */
    double getRangedThreshold();

    /**
     * Minimum consecutive cycles an archetype must be recommended before it is committed.
     *
     * @return hysteresis cycle count ≥ 1
     */
    int getArchetypeHysteresisCycles();
}
