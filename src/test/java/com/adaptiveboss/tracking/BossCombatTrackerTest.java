package com.adaptiveboss.tracking;

import com.adaptiveboss.adaptation.AdaptationProfile;
import com.adaptiveboss.adaptation.BossArchetype;
import com.adaptiveboss.adaptation.BossContext;
import com.adaptiveboss.config.TrackerConfig;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BossCombatTracker}.
 *
 * <p>A plain {@link TrackerConfig} stub and a lambda sink are used so that no
 * Minecraft runtime is required. Reflection is used in one test only to
 * exercise the defensive {@code adaptation == null} branch that guards against
 * a concurrent removal race between the two internal maps.
 */
class BossCombatTrackerTest {

    // ── Config stubs ──────────────────────────────────────────────────────────

    /**
     * Weighted config — damage-based weighting enabled, decay factor 1.0 so
     * weights remain stable across cycles.
     */
    private static final TrackerConfig WEIGHTED = new TrackerConfig() {

        @Override
        public boolean isWeightHitsByDamage() {
            return true;
        }

        @Override
        public int getSessionExpiryTicks() {
            return 200;
        }

        @Override
        public double getDecayFactor() {
            return 1.0;
        }

        @Override
        public double getMeleeThreshold() {
            return 0.60;
        }

        @Override
        public double getRangedThreshold() {
            return 0.60;
        }

        @Override
        public int getArchetypeHysteresisCycles() {
            return 1;
        }
    };

    /**
     * Unweighted config — every hit counts as {@code 1.0} regardless of damage.
     */
    private static final TrackerConfig UNWEIGHTED = new TrackerConfig() {

        @Override
        public boolean isWeightHitsByDamage() {
            return false;
        }

        @Override
        public int getSessionExpiryTicks() {
            return 200;
        }

        @Override
        public double getDecayFactor() {
            return 1.0;
        }

        @Override
        public double getMeleeThreshold() {
            return 0.60;
        }

        @Override
        public double getRangedThreshold() {
            return 0.60;
        }

        @Override
        public int getArchetypeHysteresisCycles() {
            return 1;
        }
    };

    // ── Test state ────────────────────────────────────────────────────────────

    private final AtomicReference<BossContext> lastCtx = new AtomicReference<>();

    private BossCombatTracker tracker;
    private UUID bossId;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        lastCtx.set(null);
        tracker  = new BossCombatTracker(WEIGHTED, (id, ctx) -> lastCtx.set(ctx));
        bossId   = UUID.randomUUID();
        playerId = UUID.randomUUID();
    }

    // ── startTracking ────────────────────────────────────────────────────────

    @Test
    void startTracking_makesIsTrackedTrue() {
        assertFalse(tracker.isTracked(bossId));
        tracker.startTracking(bossId);
        assertTrue(tracker.isTracked(bossId));
    }

    @Test
    void startTracking_createsBalancedAdaptationProfile() {
        tracker.startTracking(bossId);
        final AdaptationProfile profile = tracker.getAdaptationProfile(bossId);
        assertNotNull(profile);
        assertEquals(BossArchetype.BALANCED, profile.getCurrentArchetype());
    }

    @Test
    void startTracking_idempotent_doesNotResetProfile() {
        tracker.startTracking(bossId);
        // Force a non-BALANCED archetype via one heavy melee cycle
        for (int i = 0; i < 20; i++) {
            tracker.recordHit(bossId, playerId, DamageType.MELEE, 8.0, 2.0, 1);
        }
        tracker.aggregateAndAdapt(1);
        final BossArchetype before =
                tracker.getAdaptationProfile(bossId).getCurrentArchetype();

        tracker.startTracking(bossId); // second call — must be a no-op
        assertEquals(before, tracker.getAdaptationProfile(bossId).getCurrentArchetype());
    }

    @Test
    void startTracking_appearsInTrackedBossIds() {
        final UUID bossId2 = UUID.randomUUID();
        tracker.startTracking(bossId);
        tracker.startTracking(bossId2);
        assertTrue(tracker.getTrackedBossIds().contains(bossId));
        assertTrue(tracker.getTrackedBossIds().contains(bossId2));
    }

    // ── recordHit ────────────────────────────────────────────────────────────

    @Test
    void recordHit_silentlyIgnoredWhenBossNotTracked() {
        // No startTracking — should not throw or implicitly register the boss
        tracker.recordHit(bossId, playerId, DamageType.MELEE, 5.0, 2.0, 1);
        assertFalse(tracker.isTracked(bossId));
    }

    @Test
    void recordHit_weightedByDamageWhenEnabled() {
        tracker.startTracking(bossId);
        tracker.recordHit(bossId, playerId, DamageType.MELEE, 10.0, 2.0, 1);
        tracker.aggregateAndAdapt(1);
        assertNotNull(lastCtx.get());
        assertEquals(10.0, lastCtx.get().party().getMeleeWeight(), 1e-6);
    }

    @Test
    void recordHit_unitWeightWhenWeightingDisabled() {
        tracker = new BossCombatTracker(UNWEIGHTED, (id, ctx) -> lastCtx.set(ctx));
        tracker.startTracking(bossId);
        tracker.recordHit(bossId, playerId, DamageType.RANGED, 99.0, 5.0, 1);
        tracker.aggregateAndAdapt(1);
        assertNotNull(lastCtx.get());
        // Ranged weight should be 1.0 (one hit), not 99.0
        assertEquals(1.0, lastCtx.get().party().getRangedWeight(), 1e-6);
    }

    // ── hasActiveAttackers ───────────────────────────────────────────────────

    @Test
    void hasActiveAttackers_falseWhenBossNotTracked() {
        assertFalse(tracker.hasActiveAttackers(bossId, 1));
    }

    @Test
    void hasActiveAttackers_falseWhenAllSessionsExpired() {
        tracker.startTracking(bossId);
        tracker.recordHit(bossId, playerId, DamageType.MELEE, 5.0, 2.0, 1);
        // Tick 202 → (202 - 1) = 201 > expiry of 200 → expired
        assertFalse(tracker.hasActiveAttackers(bossId, 202));
    }

    @Test
    void hasActiveAttackers_trueWhenActiveSessionExists() {
        tracker.startTracking(bossId);
        tracker.recordHit(bossId, playerId, DamageType.MELEE, 5.0, 2.0, 100);
        assertTrue(tracker.hasActiveAttackers(bossId, 100));
    }

    // ── aggregateAndAdapt ────────────────────────────────────────────────────

    @Test
    void aggregateAndAdapt_doesNotCallSinkForInactiveBoss() {
        tracker.startTracking(bossId);
        // No hits → hasActiveAttackers is false → sink must not be called
        tracker.aggregateAndAdapt(1);
        assertNull(lastCtx.get());
    }

    @Test
    void aggregateAndAdapt_callsSinkWhenActiveAttackersExist() {
        tracker.startTracking(bossId);
        tracker.recordHit(bossId, playerId, DamageType.MELEE, 5.0, 2.0, 1);
        tracker.aggregateAndAdapt(1);
        assertNotNull(lastCtx.get());
    }

    @Test
    void aggregateAndAdapt_decaysSessionWeightsEachCycle() {
        final TrackerConfig decaying = new TrackerConfig() {

            @Override
            public boolean isWeightHitsByDamage() {
                return true;
            }

            @Override
            public int getSessionExpiryTicks() {
                return 200;
            }

            @Override
            public double getDecayFactor() {
                return 0.5;
            }

            @Override
            public double getMeleeThreshold() {
                return 0.60;
            }

            @Override
            public double getRangedThreshold() {
                return 0.60;
            }

            @Override
            public int getArchetypeHysteresisCycles() {
                return 1;
            }
        };
        tracker = new BossCombatTracker(decaying, (id, ctx) -> lastCtx.set(ctx));
        tracker.startTracking(bossId);
        tracker.recordHit(bossId, playerId, DamageType.MELEE, 10.0, 2.0, 1);

        tracker.aggregateAndAdapt(1);
        final double firstWeight = lastCtx.get().party().getMeleeWeight();

        tracker.aggregateAndAdapt(1);
        final double secondWeight = lastCtx.get().party().getMeleeWeight();

        assertTrue(secondWeight < firstWeight,
                "Weight should decrease after a decay cycle");
    }

    @Test
    void aggregateAndAdapt_commitsKiterAfterHeavyMeleeWithHysteresisOne() {
        tracker.startTracking(bossId);
        for (int i = 0; i < 20; i++) {
            tracker.recordHit(bossId, playerId, DamageType.MELEE, 8.0, 2.0, 1);
        }
        tracker.aggregateAndAdapt(1);
        assertEquals(BossArchetype.KITER,
                tracker.getAdaptationProfile(bossId).getCurrentArchetype());
    }

    @Test
    void aggregateAndAdapt_skipsWhenAdaptationProfileMissingForBoss()
            throws Exception {
        // Simulates a concurrent removal race: fights still has the boss entry
        // but adaptations was removed just before the cycle runs.
        tracker.startTracking(bossId);
        tracker.recordHit(bossId, playerId, DamageType.MELEE, 5.0, 2.0, 1);

        final Field field = BossCombatTracker.class.getDeclaredField("adaptations");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<UUID, AdaptationProfile> adaptations =
                (Map<UUID, AdaptationProfile>) field.get(tracker);
        adaptations.remove(bossId);

        tracker.aggregateAndAdapt(1);
        assertNull(lastCtx.get(), "Sink must not be called when profile is missing");
    }

    // ── removeBoss ───────────────────────────────────────────────────────────

    @Test
    void removeBoss_clearsAllState() {
        tracker.startTracking(bossId);
        tracker.recordHit(bossId, playerId, DamageType.MELEE, 5.0, 2.0, 1);
        tracker.removeBoss(bossId);
        assertFalse(tracker.isTracked(bossId));
        assertNull(tracker.getAdaptationProfile(bossId));
    }

    @Test
    void removeBoss_unknownIdDoesNotThrow() {
        assertDoesNotThrow(() -> tracker.removeBoss(UUID.randomUUID()));
    }

    // ── getActivePlayers ─────────────────────────────────────────────────────

    @Test
    void getActivePlayers_emptySetForUntrackedBoss() {
        assertTrue(tracker.getActivePlayers(bossId, 1).isEmpty());
    }

    @Test
    void getActivePlayers_excludesExpiredSessions() {
        tracker.startTracking(bossId);
        tracker.recordHit(bossId, playerId, DamageType.MELEE, 5.0, 2.0, 1);
        assertTrue(tracker.getActivePlayers(bossId, 202).isEmpty());
    }

    @Test
    void getActivePlayers_includesNonExpiredSessions() {
        tracker.startTracking(bossId);
        tracker.recordHit(bossId, playerId, DamageType.MELEE, 5.0, 2.0, 1);
        assertTrue(tracker.getActivePlayers(bossId, 1).contains(playerId));
    }

    // ── getAdaptationProfile / getTrackedBossIds ──────────────────────────────

    @Test
    void getAdaptationProfile_nullForUntrackedBoss() {
        assertNull(tracker.getAdaptationProfile(bossId));
    }

    @Test
    void getTrackedBossIds_returnsViewContainingTrackedBoss() {
        tracker.startTracking(bossId);
        assertTrue(tracker.getTrackedBossIds().contains(bossId));
    }
}
