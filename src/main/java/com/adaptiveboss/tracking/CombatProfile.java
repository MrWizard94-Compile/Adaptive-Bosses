package com.adaptiveboss.tracking;

import org.jetbrains.annotations.NotNull;

/**
 * Accumulates weighted combat statistics for a single player against a single boss.
 *
 * <p>Hit weights are scaled by actual damage dealt when weighting is enabled,
 * making a full-power blow count more than a chip shot.
 */
public final class CombatProfile {

    private double meleeWeight;
    private double rangedWeight;
    private double magicWeight;
    private double totalDistance;
    private int    hitCount;
    private int    lastHitTick;

    /** Creates an empty profile. */
    public CombatProfile() {
        this.meleeWeight   = 0.0;
        this.rangedWeight  = 0.0;
        this.magicWeight   = 0.0;
        this.totalDistance = 0.0;
        this.hitCount      = 0;
        this.lastHitTick   = -1;
    }

    /**
     * Records a hit of the given type, weighted by the damage amount.
     *
     * @param type         the category of damage
     * @param damageAmount the actual damage dealt (must be &gt; 0)
     * @param distance     distance from attacker to boss at time of hit
     * @param currentTick  server tick at time of hit
     */
    public void recordHit(
            @NotNull final DamageType type,
            final double damageAmount,
            final double distance,
            final int currentTick) {
        final double weight = Math.max(0.0, damageAmount);
        if (type == DamageType.MELEE) {
            meleeWeight += weight;
        } else if (type == DamageType.RANGED) {
            rangedWeight += weight;
        } else {
            magicWeight += weight;
        }
        totalDistance += distance;
        hitCount++;
        lastHitTick = currentTick;
    }

    /**
     * Applies exponential decay to all weights, keeping the profile responsive
     * to recent behavior without reacting to single bursts.
     *
     * @param factor decay multiplier in (0, 1] — e.g. 0.85 decays ~15% per cycle
     */
    public void decay(final double factor) {
        meleeWeight  *= factor;
        rangedWeight *= factor;
        magicWeight  *= factor;
        totalDistance *= factor;
    }

    /**
     * Returns {@code true} when no hits have been recorded yet.
     *
     * @return {@code true} if the profile has never been updated
     */
    public boolean isEmpty() {
        return hitCount == 0;
    }

    /** Returns the accumulated melee damage weight. */
    public double getMeleeWeight() {
        return meleeWeight;
    }

    /** Returns the accumulated ranged damage weight. */
    public double getRangedWeight() {
        return rangedWeight;
    }

    /** Returns the accumulated magic damage weight. */
    public double getMagicWeight() {
        return magicWeight;
    }

    /** Returns the total distance summed across all recorded hits. */
    public double getTotalDistance() {
        return totalDistance;
    }

    /** Returns the number of individual hits recorded. */
    public int getHitCount() {
        return hitCount;
    }

    /** Returns the server tick of the most recent hit, or {@code -1} if none. */
    public int getLastHitTick() {
        return lastHitTick;
    }
}
