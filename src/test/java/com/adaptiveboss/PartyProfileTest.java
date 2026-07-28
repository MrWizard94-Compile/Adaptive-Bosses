package com.adaptiveboss;

import com.adaptiveboss.adaptation.BossArchetype;
import com.adaptiveboss.tracking.DamageType;
import com.adaptiveboss.tracking.FightSession;
import com.adaptiveboss.tracking.PartyProfile;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Tests for {@link PartyProfile} aggregation and archetype selection. */
class PartyProfileTest {

    private static final double DELTA = 1e-9;
    private static final int    EXPIRY = 200;
    private static final double MELEE_THRESH  = 0.60;
    private static final double RANGED_THRESH = 0.60;

    // ── Archetype selection ───────────────────────────────────────────────────

    @Test
    void aggregate_emptyCollection_returnsBalanced() {
        final PartyProfile p = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertEquals(BossArchetype.BALANCED, p.determineBestArchetype(MELEE_THRESH, RANGED_THRESH));
    }

    @Test
    void aggregate_allSessionsExpired_returnsBalanced() {
        final FightSession s = new FightSession(0);
        s.recordHit(DamageType.MELEE, 100.0, 2.0, 0);
        // currentTick 500 with expiry 200 → 500 - 0 = 500 > 200 → expired
        final PartyProfile p = PartyProfile.aggregate(List.of(s), 500, EXPIRY);
        assertEquals(BossArchetype.BALANCED, p.determineBestArchetype(MELEE_THRESH, RANGED_THRESH));
    }

    @Test
    void aggregate_singleMeleePlayer_returnsKiter() {
        final FightSession s = meleeDominantSession();
        final PartyProfile p = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        assertEquals(BossArchetype.KITER, p.determineBestArchetype(MELEE_THRESH, RANGED_THRESH));
    }

    @Test
    void aggregate_singleRangedPlayer_returnsAggressor() {
        final FightSession s = rangedDominantSession();
        final PartyProfile p = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        assertEquals(BossArchetype.AGGRESSOR, p.determineBestArchetype(MELEE_THRESH, RANGED_THRESH));
    }

    @Test
    void aggregate_mixedTwoPlayers_returnsUnpredictable() {
        final FightSession melee  = meleeDominantSession();
        final FightSession ranged = rangedDominantSession();
        final PartyProfile p = PartyProfile.aggregate(List.of(melee, ranged), 0, EXPIRY);
        assertEquals(BossArchetype.UNPREDICTABLE, p.determineBestArchetype(MELEE_THRESH, RANGED_THRESH));
    }

    @Test
    void aggregate_meleeRangedEqual50_50_returnsUnpredictable() {
        final FightSession s = new FightSession(0);
        s.recordHit(DamageType.MELEE,  5.0, 2.0, 1);
        s.recordHit(DamageType.RANGED, 5.0, 8.0, 2);
        final PartyProfile p = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        assertEquals(BossArchetype.UNPREDICTABLE, p.determineBestArchetype(MELEE_THRESH, RANGED_THRESH));
    }

    @Test
    void aggregate_expiredSessionsExcluded_returnsBalanced() {
        // Session last hit at tick 0; aggregating at tick 300 with expiry 200 → expired
        final FightSession old = new FightSession(0);
        old.recordHit(DamageType.MELEE, 100.0, 1.0, 0);

        // Active session with no hits (empty) contributing at tick 300
        final FightSession active = new FightSession(300);

        final PartyProfile p = PartyProfile.aggregate(List.of(old, active), 300, EXPIRY);
        assertEquals(BossArchetype.BALANCED, p.determineBestArchetype(MELEE_THRESH, RANGED_THRESH));
    }

    // ── Exact boundary tests ──────────────────────────────────────────────────

    @Test
    void meleeRatioExactly0_60_returnsKiter() {
        final FightSession s = new FightSession(0);
        s.recordHit(DamageType.MELEE,  6.0, 2.0, 1); // 6 / 10 = 0.60
        s.recordHit(DamageType.RANGED, 4.0, 8.0, 2);
        final PartyProfile p = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        assertEquals(BossArchetype.KITER, p.determineBestArchetype(MELEE_THRESH, RANGED_THRESH));
    }

    @Test
    void meleeRatio0_59_returnsUnpredictable() {
        final FightSession s = new FightSession(0);
        // Use large numbers to get precise ratio — 59 / 100 = 0.59
        s.recordHit(DamageType.MELEE,  59.0, 2.0, 1);
        s.recordHit(DamageType.RANGED, 41.0, 8.0, 2);
        final PartyProfile p = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        assertEquals(BossArchetype.UNPREDICTABLE, p.determineBestArchetype(MELEE_THRESH, RANGED_THRESH));
    }

    @Test
    void rangedRatioExactly0_60_returnsAggressor() {
        final FightSession s = new FightSession(0);
        s.recordHit(DamageType.RANGED, 6.0, 8.0, 1); // 6 / 10 = 0.60
        s.recordHit(DamageType.MELEE,  4.0, 2.0, 2);
        final PartyProfile p = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        assertEquals(BossArchetype.AGGRESSOR, p.determineBestArchetype(MELEE_THRESH, RANGED_THRESH));
    }

    // ── Distance averaging ───────────────────────────────────────────────────

    @Test
    void avgDistance_correctlyAveragedAcrossActivePlayers() {
        final FightSession s1 = new FightSession(0);
        s1.recordHit(DamageType.MELEE, 5.0, 2.0, 1); // avg distance = 2.0

        final FightSession s2 = new FightSession(0);
        s2.recordHit(DamageType.RANGED, 5.0, 8.0, 2); // avg distance = 8.0

        final PartyProfile p = PartyProfile.aggregate(List.of(s1, s2), 0, EXPIRY);
        // Expected: (2.0 + 8.0) / 2 = 5.0
        assertEquals(5.0, p.getAvgDistance(), DELTA);
        assertEquals(2, p.getActivePlayers());
    }

    @Test
    void avgDistance_zeroWhenNoActivePlayers() {
        final PartyProfile p = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertEquals(0.0, p.getAvgDistance(), DELTA);
    }

    // ── Weight accessors ─────────────────────────────────────────────────────

    @Test
    void weightsAreAggregatedAcrossSessions() {
        final FightSession s1 = new FightSession(0);
        s1.recordHit(DamageType.MELEE, 3.0, 1.0, 1);

        final FightSession s2 = new FightSession(0);
        s2.recordHit(DamageType.MELEE, 7.0, 1.0, 2);

        final PartyProfile p = PartyProfile.aggregate(List.of(s1, s2), 0, EXPIRY);
        assertEquals(10.0, p.getMeleeWeight(), DELTA);
    }

    @Test
    void rangedAndMagicWeightGetters_returnCorrectValues() {
        final FightSession s = new FightSession(0);
        s.recordHit(DamageType.RANGED, 3.0, 1.0, 0);
        s.recordHit(DamageType.MAGIC,  7.0, 1.0, 0);
        final PartyProfile p = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        assertEquals(3.0, p.getRangedWeight(), DELTA);
        assertEquals(7.0, p.getMagicWeight(),  DELTA);
    }

    // ── equals / hashCode ────────────────────────────────────────────────────

    @Test
    void equals_sameReference_returnsTrue() {
        final PartyProfile p = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertEquals(p, p);
    }

    @Test
    void equals_null_returnsFalse() {
        final PartyProfile p = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertNotEquals(p, null);
    }

    @Test
    void equals_differentType_returnsFalse() {
        final PartyProfile p = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertNotEquals(p, "string");
    }

    @Test
    void equals_differentMeleeWeight_returnsFalse() {
        final FightSession s = new FightSession(0);
        s.recordHit(DamageType.MELEE, 5.0, 1.0, 0);
        final PartyProfile p1 = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        final PartyProfile p2 = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertNotEquals(p1, p2);
    }

    @Test
    void equals_sameMLDifferentRangedWeight_returnsFalse() {
        final FightSession s = new FightSession(0);
        s.recordHit(DamageType.RANGED, 5.0, 1.0, 0);
        // melee=0 (same as empty), ranged=5 vs 0 → false at second comparison
        final PartyProfile p1 = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        final PartyProfile p2 = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertNotEquals(p1, p2);
    }

    @Test
    void equals_sameMRDifferentMagicWeight_returnsFalse() {
        final FightSession s = new FightSession(0);
        s.recordHit(DamageType.MAGIC, 5.0, 1.0, 0);
        // melee=0, ranged=0 (same as empty), magic=5 vs 0 → false at third comparison
        final PartyProfile p1 = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        final PartyProfile p2 = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertNotEquals(p1, p2);
    }

    @Test
    void equals_sameMRMDifferentAvgDistance_returnsFalse() {
        // 0-damage hit at distance 5.0 → weights=0 (same as empty), avgDistance=5.0 vs 0.0
        final FightSession s = new FightSession(0);
        s.recordHit(DamageType.MELEE, 0.0, 5.0, 0);
        final PartyProfile p1 = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        final PartyProfile p2 = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertNotEquals(p1, p2);
    }

    @Test
    void equals_sameWeightsAndDistanceDifferentActivePlayers_returnsFalse() {
        // 0-damage hit at distance 0.0 → weights=0, avgDistance=0 (same as empty), activePlayers=1 vs 0
        final FightSession s = new FightSession(0);
        s.recordHit(DamageType.MELEE, 0.0, 0.0, 0);
        final PartyProfile p1 = PartyProfile.aggregate(List.of(s), 0, EXPIRY);
        final PartyProfile p2 = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertNotEquals(p1, p2);
    }

    @Test
    void hashCode_equalObjectsHaveEqualHashCode() {
        final PartyProfile p1 = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        final PartyProfile p2 = PartyProfile.aggregate(List.of(), 0, EXPIRY);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @NotNull
    private static FightSession meleeDominantSession() {
        final FightSession s = new FightSession(0);
        // 70% melee
        s.recordHit(DamageType.MELEE,  7.0, 2.0, 1);
        s.recordHit(DamageType.RANGED, 3.0, 8.0, 2);
        return s;
    }

    @NotNull
    private static FightSession rangedDominantSession() {
        final FightSession s = new FightSession(0);
        // 70% ranged
        s.recordHit(DamageType.RANGED, 7.0, 10.0, 1);
        s.recordHit(DamageType.MELEE,  3.0,  2.0, 2);
        return s;
    }
}
