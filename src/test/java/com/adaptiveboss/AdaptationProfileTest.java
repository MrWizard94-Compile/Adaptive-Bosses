package com.adaptiveboss;

import com.adaptiveboss.adaptation.AdaptationProfile;
import com.adaptiveboss.adaptation.BossArchetype;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Tests for {@link AdaptationProfile} hysteresis logic. */
class AdaptationProfileTest {

    private AdaptationProfile profile;

    @BeforeEach
    void setUp() {
        profile = new AdaptationProfile();
    }

    @Test
    void initialState_isBalanced() {
        assertEquals(BossArchetype.BALANCED, profile.getCurrentArchetype());
        assertEquals(BossArchetype.BALANCED, profile.getPendingArchetype());
        assertEquals(0, profile.getConsecutiveCycles());
    }

    @Test
    void recordCycle_doesNotCommitOnFirstCycle_withHysteresis2() {
        profile.recordCycle(BossArchetype.KITER, 2);
        // First cycle starts the counter but does not commit
        assertEquals(BossArchetype.BALANCED, profile.getCurrentArchetype());
        assertEquals(BossArchetype.KITER,    profile.getPendingArchetype());
        assertEquals(1, profile.getConsecutiveCycles());
    }

    @Test
    void recordCycle_commitsAfterHysteresisCycles() {
        profile.recordCycle(BossArchetype.KITER, 2);
        profile.recordCycle(BossArchetype.KITER, 2);
        assertEquals(BossArchetype.KITER, profile.getCurrentArchetype());
        assertEquals(2, profile.getConsecutiveCycles());
    }

    @Test
    void recordCycle_resetCounterOnArchetypeChange() {
        profile.recordCycle(BossArchetype.KITER,    2);
        profile.recordCycle(BossArchetype.AGGRESSOR, 2);
        // Counter reset; pending is now AGGRESSOR at 1 cycle
        assertEquals(BossArchetype.BALANCED,    profile.getCurrentArchetype());
        assertEquals(BossArchetype.AGGRESSOR,   profile.getPendingArchetype());
        assertEquals(1, profile.getConsecutiveCycles());
    }

    @Test
    void recordCycle_immediateCommitWhenHysteresisIs1() {
        profile.recordCycle(BossArchetype.KITER, 1);
        assertEquals(BossArchetype.KITER, profile.getCurrentArchetype());
        assertEquals(1, profile.getConsecutiveCycles());
    }

    @Test
    void recordCycle_immediateCommitOnChange_withHysteresis1() {
        profile.recordCycle(BossArchetype.KITER,     1);
        profile.recordCycle(BossArchetype.AGGRESSOR, 1);
        assertEquals(BossArchetype.AGGRESSOR, profile.getCurrentArchetype());
    }

    @Test
    void recordCycle_sameArchetypeRepeatedly_incrementsCounter() {
        profile.recordCycle(BossArchetype.UNPREDICTABLE, 3);
        profile.recordCycle(BossArchetype.UNPREDICTABLE, 3);
        profile.recordCycle(BossArchetype.UNPREDICTABLE, 3);
        assertEquals(BossArchetype.UNPREDICTABLE, profile.getCurrentArchetype());
        assertEquals(3, profile.getConsecutiveCycles());
    }

    // ── equals / hashCode ─────────────────────────────────────────────────────

    @Test
    void equals_sameReference_returnsTrue() {
        assertEquals(profile, profile);
    }

    @Test
    void equals_null_returnsFalse() {
        assertNotEquals(profile, null);
    }

    @Test
    void equals_differentType_returnsFalse() {
        assertNotEquals(profile, "not an AdaptationProfile");
    }

    @Test
    void equals_differentCurrentArchetype_returnsFalse() {
        // profile: currentArchetype = KITER (hysteresis 1 → immediate commit)
        profile.recordCycle(BossArchetype.KITER, 1);
        final AdaptationProfile other = new AdaptationProfile(); // currentArchetype = BALANCED
        assertNotEquals(profile, other);
    }

    @Test
    void equals_sameCurrentDifferentPending_returnsFalse() {
        // profile: current=BALANCED (not yet committed), pending=KITER, cycles=1
        profile.recordCycle(BossArchetype.KITER, 2);
        final AdaptationProfile other = new AdaptationProfile(); // pending=BALANCED
        // currentArchetype == BALANCED (same), pendingArchetype KITER != BALANCED → false
        assertNotEquals(profile, other);
    }

    @Test
    void equals_sameCurrentAndPendingDifferentCycles_returnsFalse() {
        // profile: current=BALANCED, pending=KITER, cycles=2 (after 2 cycles, hysteresis=3)
        profile.recordCycle(BossArchetype.KITER, 3);
        profile.recordCycle(BossArchetype.KITER, 3);
        final AdaptationProfile other = new AdaptationProfile();
        other.recordCycle(BossArchetype.KITER, 3); // cycles=1
        // current(BALANCED==BALANCED✓), pending(KITER==KITER✓), cycles(2!=1→false)
        assertNotEquals(profile, other);
    }

    @Test
    void hashCode_equalObjectsHaveEqualHashCode() {
        final AdaptationProfile other = new AdaptationProfile();
        assertEquals(profile.hashCode(), other.hashCode());
    }

    // ── misc ─────────────────────────────────────────────────────────────────

    @Test
    void recordCycle_commitStaysAfterChange_thenReverts() {
        // Commit KITER
        profile.recordCycle(BossArchetype.KITER, 1);
        assertEquals(BossArchetype.KITER, profile.getCurrentArchetype());

        // One cycle of AGGRESSOR (hysteresis=2) should not change current
        profile.recordCycle(BossArchetype.AGGRESSOR, 2);
        assertEquals(BossArchetype.KITER, profile.getCurrentArchetype());

        // Second cycle commits AGGRESSOR
        profile.recordCycle(BossArchetype.AGGRESSOR, 2);
        assertEquals(BossArchetype.AGGRESSOR, profile.getCurrentArchetype());
    }
}
