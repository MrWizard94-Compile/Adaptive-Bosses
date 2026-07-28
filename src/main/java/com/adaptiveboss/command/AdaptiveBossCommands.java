package com.adaptiveboss.command;

import com.adaptiveboss.adaptation.AdaptationProfile;
import com.adaptiveboss.adaptation.BossArchetype;
import com.adaptiveboss.adaptation.AdaptationEngine;
import com.adaptiveboss.tracking.BossCombatTracker;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Registers the {@code /adaptiveboss} debug command tree.
 *
 * <ul>
 *   <li>{@code /adaptiveboss stats} — lists all tracked bosses with their current archetype
 *   <li>{@code /adaptiveboss stats <uuid>} — detailed context for one boss
 *   <li>{@code /adaptiveboss force <archetype> <uuid>} — force an archetype immediately
 *   <li>{@code /adaptiveboss debug toggle} — toggles verbose per-session logging
 * </ul>
 *
 * <p>All sub-commands require operator level 2.
 */
public final class AdaptiveBossCommands {

    private final BossCombatTracker tracker;
    private final AdaptationEngine  engine;
    private boolean debugEnabled;

    /**
     * Creates a command handler.
     *
     * @param tracker the combat tracker
     * @param engine  the adaptation engine
     */
    public AdaptiveBossCommands(
            @NotNull final BossCombatTracker tracker,
            @NotNull final AdaptationEngine engine) {
        this.tracker      = tracker;
        this.engine       = engine;
        this.debugEnabled = false;
    }

    /**
     * Registers all sub-commands on the {@link RegisterCommandsEvent}.
     *
     * @param event the command registration event
     */
    @SubscribeEvent
    public void onRegisterCommands(@NotNull final RegisterCommandsEvent event) {
        final LiteralArgumentBuilder<CommandSourceStack> root =
                Commands.literal("adaptiveboss")
                        .requires(src -> src.hasPermission(2));

        // /adaptiveboss stats
        root.then(Commands.literal("stats")
                .executes(ctx -> executeStats(ctx.getSource()))
                .then(Commands.argument("uuid", StringArgumentType.word())
                        .executes(ctx -> executeStatsForBoss(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "uuid")))));

        // /adaptiveboss force <archetype> <uuid>
        root.then(Commands.literal("force")
                .then(Commands.argument("archetype", StringArgumentType.word())
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .executes(ctx -> executeForce(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "archetype"),
                                        StringArgumentType.getString(ctx, "uuid"))))));

        // /adaptiveboss debug toggle
        root.then(Commands.literal("debug")
                .then(Commands.literal("toggle")
                        .executes(ctx -> executeDebugToggle(ctx.getSource()))));

        event.getDispatcher().register(root);
    }

    private int executeStats(@NotNull final CommandSourceStack source) {
        final var ids = tracker.getTrackedBossIds();
        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("[AdaptiveBoss] No bosses currently tracked."), false);
            return 0;
        }
        for (final UUID id : ids) {
            final AdaptationProfile profile = tracker.getAdaptationProfile(id);
            final String archetype = profile != null
                    ? profile.getCurrentArchetype().name()
                    : "UNKNOWN";
            source.sendSuccess(() -> Component.literal(
                    "[AdaptiveBoss] " + id.toString().substring(0, 8) + "... → " + archetype), false);
        }
        return ids.size();
    }

    private int executeStatsForBoss(
            @NotNull final CommandSourceStack source,
            @NotNull final String uuidStr) {
        final UUID id;
        try {
            id = UUID.fromString(uuidStr);
        } catch (final IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid UUID: " + uuidStr));
            return 0;
        }
        final AdaptationProfile profile = tracker.getAdaptationProfile(id);
        if (profile == null) {
            source.sendFailure(Component.literal("Boss " + uuidStr + " is not tracked."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "[AdaptiveBoss] " + id.toString().substring(0, 8) + "..."
                + " archetype=" + profile.getCurrentArchetype()
                + " pending=" + profile.getPendingArchetype()
                + " cycles=" + profile.getConsecutiveCycles()), false);
        return 1;
    }

    private int executeForce(
            @NotNull final CommandSourceStack source,
            @NotNull final String archetypeStr,
            @NotNull final String uuidStr) {
        final BossArchetype archetype;
        try {
            archetype = BossArchetype.valueOf(archetypeStr.toUpperCase());
        } catch (final IllegalArgumentException e) {
            source.sendFailure(Component.literal("Unknown archetype: " + archetypeStr
                    + ". Valid: BALANCED, KITER, AGGRESSOR, UNPREDICTABLE"));
            return 0;
        }
        final UUID id;
        try {
            id = UUID.fromString(uuidStr);
        } catch (final IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid UUID: " + uuidStr));
            return 0;
        }
        engine.forceArchetype(id, archetype);
        source.sendSuccess(() -> Component.literal(
                "[AdaptiveBoss] Forced " + id.toString().substring(0, 8) + "... → " + archetype), true);
        return 1;
    }

    private int executeDebugToggle(@NotNull final CommandSourceStack source) {
        debugEnabled = !debugEnabled;
        final boolean enabled = debugEnabled;
        source.sendSuccess(() -> Component.literal(
                "[AdaptiveBoss] Debug logging " + (enabled ? "enabled" : "disabled")), false);
        return 1;
    }
}
