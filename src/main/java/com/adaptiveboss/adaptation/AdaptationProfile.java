package com.adaptiveboss.adaptation;

import org.jetbrains.annotations.NotNull;

/**
 * Tracks the current adaptation state for a single boss, including hysteresis
 * to prevent rapid flip-flopping when the party composition is near a threshold.
 *
 * <p>A new archetype is only committed once it has been the recommended value
 * for {@code hysteresisCycles} consecutive adaptation cycles. This avoids
 * jarring mid-fight behavior changes from a single burst of mixed attacks.
 */
public final class AdaptationProfile {

    private BossArchetype currentArchetype;
    private BossArchetype pendingArchetype;
    private int           consecutiveCycles;

    /** Creates a profile in the default {@link BossArchetype#BALANCED} state. */
    public AdaptationProfile() {
        this.currentArchetype  = BossArchetype.BALANCED;
        this.pendingArchetype  = BossArchetype.BALANCED;
        this.consecutiveCycles = 0;
    }

    /**
     * Records one adaptation cycle and potentially commits a new archetype.
     *
     * <p>The archetype is committed immediately when {@code hysteresisCycles} is 1.
     *
     * @param observed         the archetype recommended by the current party profile
     * @param hysteresisCycles minimum consecutive cycles required before committing
     *                         a new archetype (must be &ge; 1)
     */
    public void recordCycle(
            @NotNull final BossArchetype observed,
            final int hysteresisCycles) {
        if (observed == pendingArchetype) {
            consecutiveCycles++;
            if (consecutiveCycles >= hysteresisCycles) {
                currentArchetype = observed;
            }
        } else {
            pendingArchetype  = observed;
            consecutiveCycles = 1;
            if (hysteresisCycles <= 1) {
                currentArchetype = observed;
            }
        }
    }

    /**
     * Returns the currently committed archetype, which drives the boss's behavior.
     *
     * @return the active {@link BossArchetype}
     */
    @NotNull
    public BossArchetype getCurrentArchetype() {
        return currentArchetype;
    }

    /**
     * Returns the archetype that was last observed but may not yet be committed.
     *
     * @return the pending {@link BossArchetype}
     */
    @NotNull
    public BossArchetype getPendingArchetype() {
        return pendingArchetype;
    }

    /**
     * Returns the number of consecutive adaptation cycles the pending archetype
     * has been observed without interruption.
     */
    public int getConsecutiveCycles() {
        return consecutiveCycles;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AdaptationProfile)) {
            return false;
        }
        final AdaptationProfile that = (AdaptationProfile) other;
        return currentArchetype  == that.currentArchetype
            && pendingArchetype   == that.pendingArchetype
            && consecutiveCycles == that.consecutiveCycles;
    }

    @Override
    public int hashCode() {
        int result = currentArchetype.hashCode();
        result = 31 * result + pendingArchetype.hashCode();
        result = 31 * result + consecutiveCycles;
        return result;
    }
}
