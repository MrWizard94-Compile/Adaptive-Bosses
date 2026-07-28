package com.adaptiveboss;

import com.adaptiveboss.adaptation.AdaptationProfile;
import com.adaptiveboss.adaptation.BossContext;
import com.adaptiveboss.tracking.PartyProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Tests for the {@link BossContext} record. */
class BossContextTest {

    @Test
    void constructor_storesPartyAndAdaptation() {
        final PartyProfile party = PartyProfile.aggregate(List.of(), 0, 200);
        final AdaptationProfile adaptation = new AdaptationProfile();
        final BossContext ctx = new BossContext(party, adaptation);
        assertSame(party,      ctx.party());
        assertSame(adaptation, ctx.adaptation());
    }

    @Test
    void record_fieldsAreNotNull() {
        final BossContext ctx = new BossContext(
                PartyProfile.aggregate(List.of(), 0, 200),
                new AdaptationProfile());
        assertNotNull(ctx.party());
        assertNotNull(ctx.adaptation());
    }

    @Test
    void record_equalityBasedOnComponents() {
        final PartyProfile party1 = PartyProfile.aggregate(List.of(), 0, 200);
        final PartyProfile party2 = PartyProfile.aggregate(List.of(), 0, 200);
        final AdaptationProfile adapt1 = new AdaptationProfile();
        final AdaptationProfile adapt2 = new AdaptationProfile();

        final BossContext ctx1 = new BossContext(party1, adapt1);
        final BossContext ctx2 = new BossContext(party2, adapt2);

        // Records with equal components must be equal
        assertEquals(ctx1, ctx2);
    }
}
