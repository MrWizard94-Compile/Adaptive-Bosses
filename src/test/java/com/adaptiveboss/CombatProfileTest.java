package com.adaptiveboss;

import com.adaptiveboss.tracking.CombatProfile;
import com.adaptiveboss.tracking.DamageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link CombatProfile}. */
class CombatProfileTest {

    private static final double DELTA = 1e-9;

    private CombatProfile profile;

    @BeforeEach
    void setUp() {
        profile = new CombatProfile();
    }

    @Test
    void isEmpty_trueWhenNoHits() {
        assertTrue(profile.isEmpty());
    }

    @Test
    void isEmpty_falseAfterHit() {
        profile.recordHit(DamageType.MELEE, 5.0, 2.0, 100);
        assertFalse(profile.isEmpty());
    }

    @Test
    void recordHit_incrementsCorrectCounter_melee() {
        profile.recordHit(DamageType.MELEE, 3.0, 1.5, 10);
        assertEquals(3.0, profile.getMeleeWeight(),  DELTA);
        assertEquals(0.0, profile.getRangedWeight(), DELTA);
        assertEquals(0.0, profile.getMagicWeight(),  DELTA);
    }

    @Test
    void recordHit_incrementsCorrectCounter_ranged() {
        profile.recordHit(DamageType.RANGED, 7.0, 10.0, 20);
        assertEquals(0.0, profile.getMeleeWeight(),  DELTA);
        assertEquals(7.0, profile.getRangedWeight(), DELTA);
        assertEquals(0.0, profile.getMagicWeight(),  DELTA);
    }

    @Test
    void recordHit_incrementsCorrectCounter_magic() {
        profile.recordHit(DamageType.MAGIC, 4.0, 5.0, 30);
        assertEquals(0.0, profile.getMeleeWeight(),  DELTA);
        assertEquals(0.0, profile.getRangedWeight(), DELTA);
        assertEquals(4.0, profile.getMagicWeight(),  DELTA);
    }

    @Test
    void recordHit_weightsByDamageAmount() {
        profile.recordHit(DamageType.MELEE, 10.0, 1.0, 1);
        profile.recordHit(DamageType.MELEE,  1.0, 1.0, 2);
        // 10-damage hit contributes more than 1-damage hit
        assertEquals(11.0, profile.getMeleeWeight(), DELTA);
        assertEquals(2, profile.getHitCount());
    }

    @Test
    void recordHit_accumulatesDistance() {
        profile.recordHit(DamageType.MELEE, 5.0, 3.0, 1);
        profile.recordHit(DamageType.RANGED, 5.0, 7.0, 2);
        assertEquals(10.0, profile.getTotalDistance(), DELTA);
    }

    @Test
    void recordHit_updatesLastHitTick() {
        profile.recordHit(DamageType.MELEE, 5.0, 2.0, 42);
        assertEquals(42, profile.getLastHitTick());
    }

    @Test
    void recordHit_zeroDamageIsAllowed() {
        profile.recordHit(DamageType.MELEE, 0.0, 1.0, 1);
        assertEquals(0.0, profile.getMeleeWeight(), DELTA);
        assertEquals(1, profile.getHitCount());
    }

    @Test
    void decay_reducesWeightsByFactor() {
        profile.recordHit(DamageType.MELEE,  10.0, 2.0, 1);
        profile.recordHit(DamageType.RANGED,  8.0, 5.0, 2);
        profile.recordHit(DamageType.MAGIC,   4.0, 1.0, 3);
        profile.decay(0.5);
        assertEquals(5.0, profile.getMeleeWeight(),  DELTA);
        assertEquals(4.0, profile.getRangedWeight(), DELTA);
        assertEquals(2.0, profile.getMagicWeight(),  DELTA);
    }

    @Test
    void decay_reducesDistanceByFactor() {
        profile.recordHit(DamageType.MELEE, 5.0, 10.0, 1);
        profile.decay(0.5);
        assertEquals(5.0, profile.getTotalDistance(), DELTA);
    }

    @Test
    void decay_handlesZeroValues() {
        profile.decay(0.85);
        assertEquals(0.0, profile.getMeleeWeight(),  DELTA);
        assertEquals(0.0, profile.getRangedWeight(), DELTA);
        assertEquals(0.0, profile.getMagicWeight(),  DELTA);
    }

    @Test
    void getLastHitTick_minusOneWhenNoHits() {
        assertEquals(-1, profile.getLastHitTick());
    }
}
