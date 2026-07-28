package com.adaptiveboss.adaptation;

import com.adaptiveboss.tracking.PartyProfile;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable snapshot of the current adaptation state for a single boss,
 * passed through the engine and behavior layers to avoid repeated map lookups.
 *
 * @param party      the aggregated party profile for this tick
 * @param adaptation the boss's current archetype and hysteresis state
 */
public record BossContext(
        @NotNull PartyProfile party,
        @NotNull AdaptationProfile adaptation) {
}
