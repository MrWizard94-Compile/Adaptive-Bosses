package com.adaptiveboss.adaptation;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Callback invoked by {@link com.adaptiveboss.tracking.BossCombatTracker} at
 * the end of each adaptation cycle with the latest {@link BossContext}.
 *
 * <p>Implemented by {@link AdaptationEngine} in production. Unit tests supply
 * a lambda without constructing a live engine that requires a Minecraft runtime.
 */
@FunctionalInterface
public interface AdaptationSink {

    /**
     * Called once per boss per adaptation cycle after party aggregation and
     * archetype hysteresis have been applied.
     *
     * @param bossId the UUID of the boss whose cycle just completed
     * @param ctx    the current context snapshot (party profile + adaptation state)
     */
    void onAdaptationCycle(@NotNull UUID bossId, @NotNull BossContext ctx);
}
