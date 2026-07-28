package com.adaptiveboss.event;

import com.adaptiveboss.adaptation.AdaptationEngine;
import com.adaptiveboss.config.AdaptiveBossConfig;
import com.adaptiveboss.registry.AdaptiveBossRegistry;
import com.adaptiveboss.registry.AdaptiveBossTags;
import com.adaptiveboss.registry.BossDetectionHelper;
import com.adaptiveboss.tracking.BossCombatTracker;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * Handles boss entity spawn and death, wiring them into the tracking and adaptation systems.
 *
 * <p>Boss detection uses a three-tier priority:
 * <ol>
 *   <li>Explicitly registered via {@link AdaptiveBossRegistry#registerBoss}</li>
 *   <li>Listed in the {@code #adaptiveboss:bosses} entity tag</li>
 *   <li>Reflection-based auto-detection via {@link BossDetectionHelper}</li>
 * </ol>
 */
public final class BossLifecycleHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final BossCombatTracker  tracker;
    private final AdaptationEngine   engine;
    private final AdaptiveBossConfig config;

    /**
     * Creates a lifecycle handler.
     *
     * @param tracker the combat tracker
     * @param engine  the adaptation engine
     * @param config  the mod configuration
     */
    public BossLifecycleHandler(
            @NotNull final BossCombatTracker tracker,
            @NotNull final AdaptationEngine engine,
            @NotNull final AdaptiveBossConfig config) {
        this.tracker = tracker;
        this.engine  = engine;
        this.config  = config;
    }

    /**
     * Fires when any entity enters a level. Detects and initialises boss tracking.
     *
     * @param event the entity join event
     */
    @SubscribeEvent
    public void onEntityJoin(@NotNull final EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!isBoss(living)) {
            return;
        }
        // Ensure it's registered so later damage events can look it up quickly
        AdaptiveBossRegistry.registerBoss(living.getType());
        tracker.startTracking(living.getUUID());
        if (living instanceof Mob mob) {
            engine.ensureControlGoalAdded(mob);
        }
    }

    /**
     * Fires when a living entity dies. Cleans up boss tracking state.
     *
     * @param event the death event
     */
    @SubscribeEvent
    public void onLivingDeath(@NotNull final LivingDeathEvent event) {
        final var entity = event.getEntity();
        if (tracker.isTracked(entity.getUUID())) {
            tracker.removeBoss(entity.getUUID());
            engine.removeBoss(entity.getUUID());
        }
    }

    // ── Detection helpers ─────────────────────────────────────────────────────

    private boolean isBoss(@NotNull final LivingEntity entity) {
        // Tier 1: explicit API registration
        if (AdaptiveBossRegistry.isRegistered(entity.getType())) {
            return true;
        }
        // Tier 2: data-driven tag
        if (entity.getType().is(AdaptiveBossTags.BOSSES)) {
            return true;
        }
        // Tier 3: reflection-based auto-detect (looks for ServerBossEvent field)
        if (BossDetectionHelper.looksLikeBoss(entity)) {
            if (config.debugLogging.get()) {
                LOGGER.debug("[DETECT] {} auto-registered via reflection",
                        entity.getType().toShortString());
            }
            return true;
        }
        return false;
    }
}
