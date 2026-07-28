package com.adaptiveboss.event;

import com.adaptiveboss.config.AdaptiveBossConfig;
import com.adaptiveboss.tracking.BossCombatTracker;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Drives the adaptation loop by running the combat tracker's aggregate-and-adapt
 * cycle at the configured interval.
 *
 * <p>Fires after all entity {@code aiStep()} calls, which is the safe moment to
 * swap goal behaviors without conflicting with ongoing AI execution.
 */
public final class AdaptationTickHandler {

    private final BossCombatTracker  tracker;
    private final AdaptiveBossConfig config;
    private int tickCounter;

    /**
     * Creates a tick handler bound to the given tracker and config.
     *
     * @param tracker the combat tracker
     * @param config  the mod configuration
     */
    public AdaptationTickHandler(
            @NotNull final BossCombatTracker tracker,
            @NotNull final AdaptiveBossConfig config) {
        this.tracker      = tracker;
        this.config       = config;
        this.tickCounter  = 0;
    }

    /**
     * Called after every server tick. Triggers the adaptation cycle every
     * {@code adaptationInterval} ticks. Bosses with no active attackers are
     * skipped to stay O(active fights).
     *
     * @param event the post-tick event
     */
    @SubscribeEvent
    public void onServerTickPost(@NotNull final ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= config.adaptationInterval.get()) {
            final int currentTick = (int) event.getServer().getTickCount();
            tracker.aggregateAndAdapt(currentTick);
            tickCounter = 0;
        }
    }
}
