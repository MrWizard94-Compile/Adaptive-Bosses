package com.adaptiveboss.event;

import com.adaptiveboss.config.AdaptiveBossConfig;
import com.adaptiveboss.registry.AdaptiveBossRegistry;
import com.adaptiveboss.tracking.BossCombatTracker;
import com.adaptiveboss.tracking.DamageType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for damage events and records hits against tracked boss entities.
 *
 * <p>Only direct player attacks are recorded. Minion damage and spectator
 * actions are filtered out to keep profiles meaningful.
 */
public final class DamageTrackingHandler {

    private final BossCombatTracker tracker;
    private final AdaptiveBossConfig config;

    /**
     * Creates a handler bound to the given tracker and config.
     *
     * @param tracker the combat tracker to record hits into
     * @param config  the mod configuration
     */
    public DamageTrackingHandler(
            @NotNull final BossCombatTracker tracker,
            @NotNull final AdaptiveBossConfig config) {
        this.tracker = tracker;
        this.config  = config;
    }

    /**
     * Fires before immunity frames. Classifies the damage source and records
     * the hit when the target is a tracked boss and the attacker is a real player.
     *
     * @param event the damage event
     */
    @SubscribeEvent
    public void onLivingDamage(@NotNull final LivingDamageEvent.Pre event) {
        final LivingEntity target = event.getEntity();

        // Only process tracked bosses
        if (!AdaptiveBossRegistry.isRegistered(target.getType())) {
            return;
        }
        if (!tracker.isTracked(target.getUUID())) {
            return;
        }

        // Source must be a real player
        final var attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }
        if (config.filterNonRealPlayers.get() && player.isSpectator()) {
            return;
        }

        final DamageType type     = DamageType.classify(event.getSource());
        final double     damage   = event.getNewDamage();
        final double     distance = target.distanceTo(player);
        final int        tick     = (int) target.level().getGameTime();

        tracker.recordHit(target.getUUID(), player.getUUID(), type, damage, distance, tick);
    }
}
