package com.adaptiveboss;

import com.adaptiveboss.tracking.DamageType;
import com.adaptiveboss.tracking.FightSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link FightSession}. */
class FightSessionTest {

    private static final double DELTA = 1e-9;

    @Test
    void isExpired_falseWhenJustCreated() {
        final FightSession session = new FightSession(100);
        assertFalse(session.isExpired(100, 200));
    }

    @Test
    void isExpired_falseWithinExpiryWindow() {
        final FightSession session = new FightSession(0);
        assertFalse(session.isExpired(199, 200));
    }

    @Test
    void isExpired_falseAtExactExpiryBoundary() {
        // Expiry is strictly >; at exactly the limit it has not yet expired
        final FightSession session = new FightSession(0);
        assertFalse(session.isExpired(200, 200));
    }

    @Test
    void isExpired_trueAfterExpiry() {
        final FightSession session = new FightSession(0);
        assertTrue(session.isExpired(201, 200));
    }

    @Test
    void recordHit_refreshesLastHitTick() {
        final FightSession session = new FightSession(0);
        session.recordHit(DamageType.MELEE, 5.0, 2.0, 150);
        assertEquals(150, session.getLastHitTick());
        // Session should no longer expire relative to tick 150
        assertFalse(session.isExpired(200, 200));
    }

    @Test
    void recordHit_delegatesToProfile() {
        final FightSession session = new FightSession(0);
        session.recordHit(DamageType.RANGED, 8.0, 10.0, 1);
        assertEquals(8.0, session.getProfile().getRangedWeight(), DELTA);
        assertEquals(1, session.getProfile().getHitCount());
    }

    @Test
    void decay_delegatesToProfile() {
        final FightSession session = new FightSession(0);
        session.recordHit(DamageType.MELEE, 10.0, 2.0, 1);
        session.decay(0.5);
        assertEquals(5.0, session.getProfile().getMeleeWeight(), DELTA);
    }

    @Test
    void getProfile_neverNull() {
        final FightSession session = new FightSession(0);
        assertNotNull(session.getProfile());
    }
}
