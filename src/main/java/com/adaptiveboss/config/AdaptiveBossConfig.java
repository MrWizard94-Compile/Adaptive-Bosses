package com.adaptiveboss.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

/**
 * Server-side configuration for the AdaptiveBoss mod.
 *
 * <p>All values are read at runtime so a server restart propagates config
 * changes without a full restart being required for non-structural options.
 *
 * <p>Grouped into three logical categories using
 * {@link ModConfigSpec.Builder#push(String)}:
 * <ul>
 *   <li><b>adaptation</b> — how frequently and how aggressively the boss adapts
 *   <li><b>tracking</b> — how player combat data is collected and aged
 *   <li><b>debug</b> — verbose logging for development and testing
 * </ul>
 */
public final class AdaptiveBossConfig implements TrackerConfig {

    /** Adaptation category — controls the adaptation loop. */
    public final ModConfigSpec.IntValue adaptationInterval;
    /** Decay factor applied to all combat weights each cycle. */
    public final ModConfigSpec.DoubleValue decayFactor;
    /** Melee ratio at or above which the boss adopts KITER archetype. */
    public final ModConfigSpec.DoubleValue meleeThreshold;
    /** Ranged ratio at or above which the boss adopts AGGRESSOR archetype. */
    public final ModConfigSpec.DoubleValue rangedThreshold;
    /** Minimum consecutive cycles an archetype must be observed before committing. */
    public final ModConfigSpec.IntValue archetypeHysteresisCycles;

    /** Tracking category — controls session and hit recording. */
    public final ModConfigSpec.IntValue sessionExpiryTicks;
    /** Whether hits are weighted by actual damage dealt. */
    public final ModConfigSpec.BooleanValue weightHitsByDamage;
    /** Whether non-player entities (bots, fake players) are filtered from tracking. */
    public final ModConfigSpec.BooleanValue filterNonRealPlayers;

    /** Debug category. */
    public final ModConfigSpec.BooleanValue debugLogging;

    /** The built spec, registered with the mod container. */
    @NotNull
    public final ModConfigSpec spec;

    /** Constructs the config spec and all value handles. */
    public AdaptiveBossConfig() {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("adaptation");
        adaptationInterval = builder
                .comment("Ticks between each adaptation evaluation (~20 ticks = 1 second).")
                .defineInRange("adaptationInterval", 80, 20, 200);
        decayFactor = builder
                .comment("Exponential decay factor applied to combat weights each cycle (0.5–1.0).")
                .defineInRange("decayFactor", 0.85, 0.5, 1.0);
        meleeThreshold = builder
                .comment("Melee damage ratio at which the boss becomes a KITER.")
                .defineInRange("meleeThreshold", 0.60, 0.1, 1.0);
        rangedThreshold = builder
                .comment("Ranged damage ratio at which the boss becomes an AGGRESSOR.")
                .defineInRange("rangedThreshold", 0.60, 0.1, 1.0);
        archetypeHysteresisCycles = builder
                .comment("Consecutive cycles an archetype must be recommended before it is committed.")
                .defineInRange("archetypeHysteresisCycles", 2, 1, 10);
        builder.pop();

        builder.push("tracking");
        sessionExpiryTicks = builder
                .comment("Ticks of combat inactivity after which a player's session is considered expired.")
                .defineInRange("sessionExpiryTicks", 200, 20, 2000);
        weightHitsByDamage = builder
                .comment("If true, hits are weighted by actual damage dealt. If false, each hit has equal weight.")
                .define("weightHitsByDamage", true);
        filterNonRealPlayers = builder
                .comment("If true, fake players and spectators are excluded from combat tracking.")
                .define("filterNonRealPlayers", true);
        builder.pop();

        builder.push("debug");
        debugLogging = builder
                .comment("Enables verbose adaptation logging (archetype transitions, party ratios).")
                .define("debugLogging", false);
        builder.pop();

        spec = builder.build();
    }

    // ── TrackerConfig ─────────────────────────────────────────────────────────

    @Override
    public boolean isWeightHitsByDamage() {
        return weightHitsByDamage.get();
    }

    @Override
    public int getSessionExpiryTicks() {
        return sessionExpiryTicks.get();
    }

    @Override
    public double getDecayFactor() {
        return decayFactor.get();
    }

    @Override
    public double getMeleeThreshold() {
        return meleeThreshold.get();
    }

    @Override
    public double getRangedThreshold() {
        return rangedThreshold.get();
    }

    @Override
    public int getArchetypeHysteresisCycles() {
        return archetypeHysteresisCycles.get();
    }
}
