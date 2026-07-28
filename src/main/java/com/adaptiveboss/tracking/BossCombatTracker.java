package com.adaptiveboss.tracking;

import com.adaptiveboss.adaptation.AdaptationProfile;
import com.adaptiveboss.adaptation.AdaptationSink;
import com.adaptiveboss.adaptation.BossContext;
import com.adaptiveboss.config.TrackerConfig;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Central store of per-boss, per-player {@link FightSession}s.
 *
 * <p>All mutating operations are called from the server thread via events and
 * the adaptation tick handler, so the {@link ConcurrentHashMap}s here protect
 * against rare edge-cases (e.g., async world loading) rather than being the
 * primary synchronisation mechanism.
 */
public final class BossCombatTracker {

    /** Maps boss UUID to a per-player map of active fight sessions. */
    private final Map<UUID, Map<UUID, FightSession>> fights = new ConcurrentHashMap<>();
    /** Maps boss UUID to its current adaptation profile. */
    private final Map<UUID, AdaptationProfile> adaptations = new ConcurrentHashMap<>();

    private final TrackerConfig  config;
    private final AdaptationSink sink;

    /**
     * Creates a tracker bound to the given configuration and adaptation sink.
     *
     * @param config the tracker configuration (thresholds, expiry, etc.)
     * @param sink   callback invoked at the end of each adaptation cycle
     */
    public BossCombatTracker(
            @NotNull final TrackerConfig config,
            @NotNull final AdaptationSink sink) {
        this.config = config;
        this.sink   = sink;
    }

    /**
     * Registers a new boss entity for tracking, creating an empty fight map for it.
     *
     * @param bossId the UUID of the spawning boss
     */
    public void startTracking(@NotNull final UUID bossId) {
        fights.putIfAbsent(bossId, new ConcurrentHashMap<>());
        adaptations.putIfAbsent(bossId, new AdaptationProfile());
    }

    /**
     * Records a hit against a tracked boss.
     *
     * @param bossId       UUID of the boss that was hit
     * @param playerId     UUID of the attacking player
     * @param type         classified damage type
     * @param damageAmount actual damage dealt (used for weighting when configured)
     * @param distance     attacker-to-boss distance at the time of the hit
     * @param currentTick  the current server tick
     */
    public void recordHit(
            @NotNull final UUID bossId,
            @NotNull final UUID playerId,
            @NotNull final DamageType type,
            final double damageAmount,
            final double distance,
            final int currentTick) {
        final Map<UUID, FightSession> playerSessions = fights.get(bossId);
        if (playerSessions == null) {
            return;
        }
        final FightSession session = playerSessions.computeIfAbsent(
                playerId, id -> new FightSession(currentTick));
        final double weight = config.isWeightHitsByDamage() ? damageAmount : 1.0;
        session.recordHit(type, weight, distance, currentTick);
    }

    /**
     * Returns {@code true} if the given boss has at least one active (non-expired)
     * player session, indicating there is meaningful combat data to process.
     *
     * @param bossId      the boss UUID to check
     * @param currentTick the current server tick
     * @return {@code true} if any active sessions exist
     */
    public boolean hasActiveAttackers(
            @NotNull final UUID bossId,
            final int currentTick) {
        final Map<UUID, FightSession> sessions = fights.get(bossId);
        if (sessions == null) {
            return false;
        }
        final int expiry = config.getSessionExpiryTicks();
        for (final FightSession session : sessions.values()) {
            if (!session.isExpired(currentTick, expiry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs the adaptation cycle for all tracked bosses that have active attackers.
     *
     * <p>Called by {@code AdaptationTickHandler} every {@code adaptationInterval} ticks.
     * For each boss: decays profiles, aggregates a {@link PartyProfile}, updates the
     * {@link AdaptationProfile} with hysteresis, then asks the engine to update behavior.
     *
     * @param currentTick the current server tick
     */
    public void aggregateAndAdapt(final int currentTick) {
        final int    expiry      = config.getSessionExpiryTicks();
        final double decay       = config.getDecayFactor();
        final double meleeTh     = config.getMeleeThreshold();
        final double rangedTh    = config.getRangedThreshold();
        final int    hysteresis  = config.getArchetypeHysteresisCycles();

        for (final Map.Entry<UUID, Map<UUID, FightSession>> entry : fights.entrySet()) {
            final UUID bossId = entry.getKey();
            final Map<UUID, FightSession> sessions = entry.getValue();

            // Skip inactive bosses to stay O(active fights)
            if (!hasActiveAttackers(bossId, currentTick)) {
                continue;
            }

            for (final FightSession session : sessions.values()) {
                session.decay(decay);
            }

            final PartyProfile party = PartyProfile.aggregate(
                    sessions.values(), currentTick, expiry);
            final AdaptationProfile adaptation = adaptations.get(bossId);
            if (adaptation == null) {
                continue;
            }

            adaptation.recordCycle(
                    party.determineBestArchetype(meleeTh, rangedTh),
                    hysteresis);

            final BossContext ctx = new BossContext(party, adaptation);
            sink.onAdaptationCycle(bossId, ctx);
        }
    }

    /**
     * Removes all tracking state for a boss (called on death or despawn).
     *
     * @param bossId the UUID of the boss to remove
     */
    public void removeBoss(@NotNull final UUID bossId) {
        fights.remove(bossId);
        adaptations.remove(bossId);
    }

    /**
     * Returns the UUIDs of players with active (non-expired) sessions for the given boss.
     *
     * <p>Intended for use by addon mods (e.g., AdaptiveAttacks) that need to resolve
     * the live player list without direct access to internal session maps.
     *
     * @param bossId      the boss UUID
     * @param currentTick the current server tick
     * @return an unmodifiable set of active player UUIDs; empty if boss is not tracked
     */
    @NotNull
    public java.util.Set<UUID> getActivePlayers(
            @NotNull final UUID bossId,
            final int currentTick) {
        final Map<UUID, FightSession> sessions = fights.get(bossId);
        if (sessions == null) {
            return Collections.emptySet();
        }
        final int expiry = config.getSessionExpiryTicks();
        final java.util.Set<UUID> active = ConcurrentHashMap.newKeySet();
        for (final Map.Entry<UUID, FightSession> entry : sessions.entrySet()) {
            if (!entry.getValue().isExpired(currentTick, expiry)) {
                active.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(active);
    }

    /**
     * Returns whether a boss is currently being tracked.
     *
     * @param bossId the UUID to check
     * @return {@code true} if the boss is tracked
     */
    public boolean isTracked(@NotNull final UUID bossId) {
        return fights.containsKey(bossId);
    }

    /**
     * Returns an unmodifiable view of all tracked boss UUIDs.
     *
     * @return set of tracked boss UUIDs
     */
    @NotNull
    public Set<UUID> getTrackedBossIds() {
        return Collections.unmodifiableSet(fights.keySet());
    }

    /**
     * Returns the {@link AdaptationProfile} for a tracked boss, or {@code null}
     * if the boss is not tracked.
     *
     * @param bossId the UUID of the boss
     * @return the profile, or {@code null}
     */
    @org.jetbrains.annotations.Nullable
    public AdaptationProfile getAdaptationProfile(@NotNull final UUID bossId) {
        return adaptations.get(bossId);
    }
}
