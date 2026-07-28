package com.adaptiveboss.tracking;

import com.adaptiveboss.adaptation.BossArchetype;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * Aggregates active {@link FightSession}s for all players currently fighting
 * a boss into a composite party profile.
 *
 * <p>Expired sessions are excluded, so a player who disengaged long ago
 * does not skew the result. All weights are summed across players; the
 * resulting ratios drive archetype selection.
 */
public final class PartyProfile {

    private final double meleeWeight;
    private final double rangedWeight;
    private final double magicWeight;
    private final double avgDistance;
    private final int    activePlayers;

    private PartyProfile(
            final double meleeWeight,
            final double rangedWeight,
            final double magicWeight,
            final double avgDistance,
            final int activePlayers) {
        this.meleeWeight   = meleeWeight;
        this.rangedWeight  = rangedWeight;
        this.magicWeight   = magicWeight;
        this.avgDistance   = avgDistance;
        this.activePlayers = activePlayers;
    }

    /**
     * Builds a {@link PartyProfile} from a collection of sessions, filtering
     * out any that have expired.
     *
     * @param sessions           all sessions for a given boss
     * @param currentTick        the current server tick (used for expiry check)
     * @param sessionExpiryTicks maximum ticks of inactivity before a session expires
     * @return the aggregated party profile
     */
    @NotNull
    public static PartyProfile aggregate(
            @NotNull final Collection<FightSession> sessions,
            final int currentTick,
            final int sessionExpiryTicks) {
        double totalMelee    = 0.0;
        double totalRanged   = 0.0;
        double totalMagic    = 0.0;
        double totalDistance = 0.0;
        int    active        = 0;

        for (final FightSession session : sessions) {
            if (session.isExpired(currentTick, sessionExpiryTicks)) {
                continue;
            }
            final CombatProfile p = session.getProfile();
            totalMelee    += p.getMeleeWeight();
            totalRanged   += p.getRangedWeight();
            totalMagic    += p.getMagicWeight();
            if (p.getHitCount() > 0) {
                totalDistance += p.getTotalDistance() / p.getHitCount();
                active++;
            }
        }

        final double avgDist = active > 0 ? totalDistance / active : 0.0;
        return new PartyProfile(totalMelee, totalRanged, totalMagic, avgDist, active);
    }

    /**
     * Determines the best {@link BossArchetype} for the current party composition.
     *
     * @param meleeThreshold  ratio at or above which the party is considered melee-dominant
     * @param rangedThreshold ratio at or above which the party is considered ranged-dominant
     * @return the recommended archetype
     */
    @NotNull
    public BossArchetype determineBestArchetype(
            final double meleeThreshold,
            final double rangedThreshold) {
        final double total = meleeWeight + rangedWeight + magicWeight;
        if (total <= 0.0) {
            return BossArchetype.BALANCED;
        }
        final double meleeRatio  = meleeWeight  / total;
        final double rangedRatio = rangedWeight / total;
        if (meleeRatio >= meleeThreshold) {
            return BossArchetype.KITER;
        }
        if (rangedRatio >= rangedThreshold) {
            return BossArchetype.AGGRESSOR;
        }
        return BossArchetype.UNPREDICTABLE;
    }

    /** Returns the total melee damage weight across active players. */
    public double getMeleeWeight() {
        return meleeWeight;
    }

    /** Returns the total ranged damage weight across active players. */
    public double getRangedWeight() {
        return rangedWeight;
    }

    /** Returns the total magic damage weight across active players. */
    public double getMagicWeight() {
        return magicWeight;
    }

    /** Returns the average distance across active players. */
    public double getAvgDistance() {
        return avgDistance;
    }

    /** Returns the number of active (non-expired) players contributing to this profile. */
    public int getActivePlayers() {
        return activePlayers;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PartyProfile)) {
            return false;
        }
        final PartyProfile that = (PartyProfile) other;
        return Double.compare(meleeWeight,   that.meleeWeight)   == 0
            && Double.compare(rangedWeight,  that.rangedWeight)  == 0
            && Double.compare(magicWeight,   that.magicWeight)   == 0
            && Double.compare(avgDistance,   that.avgDistance)   == 0
            && activePlayers == that.activePlayers;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(meleeWeight);
        result = 31 * result + Double.hashCode(rangedWeight);
        result = 31 * result + Double.hashCode(magicWeight);
        result = 31 * result + Double.hashCode(avgDistance);
        result = 31 * result + activePlayers;
        return result;
    }
}
