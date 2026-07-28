package com.adaptiveboss;

import com.adaptiveboss.adaptation.AdaptationEngine;
import com.adaptiveboss.command.AdaptiveBossCommands;
import com.adaptiveboss.config.AdaptiveBossConfig;
import com.adaptiveboss.event.AdaptationTickHandler;
import com.adaptiveboss.event.BossLifecycleHandler;
import com.adaptiveboss.event.DamageTrackingHandler;
import com.adaptiveboss.registry.AdaptiveBossRegistry;
import com.adaptiveboss.tracking.BossCombatTracker;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Mod entry point. Wires together all components and registers event listeners.
 *
 * <p>A single static accessor pattern is used here so that mixins can reach the
 * tracker and engine without dependency injection boilerplate. All mutable
 * singleton state lives in this class and is initialised exactly once at
 * mod construction.
 */
@Mod("adaptiveboss")
public final class AdaptiveBossMod {

    /** Mod ID constant used across the codebase. */
    public static final String MOD_ID = "adaptiveboss";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static AdaptiveBossConfig config;
    private static BossCombatTracker  tracker;
    private static AdaptationEngine   engine;

    /**
     * Constructs the mod, initialising all subsystems and registering events.
     *
     * @param modEventBus the mod-lifecycle event bus
     * @param container   the mod container (used for config registration)
     */
    public AdaptiveBossMod(
            @NotNull final IEventBus modEventBus,
            @NotNull final ModContainer container) {
        config  = new AdaptiveBossConfig();
        engine  = new AdaptationEngine(config);
        tracker = new BossCombatTracker(config, engine);

        container.registerConfig(ModConfig.Type.SERVER, config.spec);

        // Register default vanilla bosses
        AdaptiveBossRegistry.registerBoss(EntityType.WITHER);
        AdaptiveBossRegistry.registerBoss(EntityType.ENDER_DRAGON);

        // Register game-thread event listeners
        NeoForge.EVENT_BUS.register(new DamageTrackingHandler(tracker, config));
        NeoForge.EVENT_BUS.register(new BossLifecycleHandler(tracker, engine, config));
        NeoForge.EVENT_BUS.register(new AdaptationTickHandler(tracker, config));
        NeoForge.EVENT_BUS.register(new AdaptiveBossCommands(tracker, engine));

        LOGGER.info("[AdaptiveBoss] Mod initialised. Wither and Ender Dragon are registered.");
    }

    /**
     * Returns the global combat tracker instance.
     *
     * @return the tracker; never {@code null} after mod initialisation
     */
    @NotNull
    public static BossCombatTracker getTracker() {
        return tracker;
    }

    /**
     * Returns the global adaptation engine instance.
     *
     * @return the engine; never {@code null} after mod initialisation
     */
    @NotNull
    public static AdaptationEngine getEngine() {
        return engine;
    }

    /**
     * Returns the global mod configuration.
     *
     * @return the config; never {@code null} after mod initialisation
     */
    @NotNull
    public static AdaptiveBossConfig getConfig() {
        return config;
    }
}
