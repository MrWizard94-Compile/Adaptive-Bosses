package com.adaptiveboss.adaptation;

import com.adaptiveboss.config.AdaptiveBossConfig;
import com.adaptiveboss.goal.AdaptiveControlGoal;
import com.adaptiveboss.goal.BalancedBehavior;
import com.adaptiveboss.goal.BossBehavior;
import com.adaptiveboss.goal.KiteBehavior;
import com.adaptiveboss.goal.RushBehavior;
import com.adaptiveboss.goal.UnpredictableBehavior;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Manages the lifecycle of the {@link AdaptiveControlGoal} on registered bosses
 * and routes archetype transitions to the correct {@link BossBehavior}.
 *
 * <p>The control goal is added once when a boss spawns and is never removed.
 * Archetype changes are handled by swapping the behavior delegate inside the
 * existing goal, avoiding any interaction with vanilla goal selector state.
 */
public final class AdaptationEngine implements AdaptationSink {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Priority at which the AdaptiveControlGoal is inserted. */
    private static final int GOAL_PRIORITY = 1;

    /** bossId → the living goal instance on that boss entity. */
    private final Map<UUID, AdaptiveControlGoal> controlGoals = new ConcurrentHashMap<>();

    /** bossId → entity reference (needed to swap behaviors on tick callback). */
    private final Map<UUID, Mob> bossMobs = new ConcurrentHashMap<>();

    private final AdaptiveBossConfig config;

    /**
     * Creates an engine bound to the given configuration.
     *
     * @param config the mod configuration; debug logging is read lazily on each use
     */
    public AdaptationEngine(@NotNull final AdaptiveBossConfig config) {
        this.config = config;
    }

    /**
     * Adds the {@link AdaptiveControlGoal} to a boss if it does not already have one.
     * Safe to call multiple times for the same boss.
     *
     * @param boss the boss entity
     */
    public void ensureControlGoalAdded(@NotNull final Mob boss) {
        final UUID id = boss.getUUID();
        if (controlGoals.containsKey(id)) {
            return;
        }
        final AdaptiveControlGoal goal = new AdaptiveControlGoal(boss);
        goal.setBehavior(new BalancedBehavior(boss));
        boss.goalSelector.addGoal(GOAL_PRIORITY, goal);
        controlGoals.put(id, goal);
        bossMobs.put(id, boss);
        if (config.debugLogging.get()) {
            LOGGER.debug("[ADAPT] {} control goal added", shortId(id));
        }
    }

    /**
     * Called each adaptation cycle with the latest {@link BossContext}.
     * Swaps the behavior on the existing control goal if the archetype changed.
     *
     * @param bossId the boss UUID
     * @param ctx    the current context snapshot
     */
    public void onAdaptationCycle(
            @NotNull final UUID bossId,
            @NotNull final BossContext ctx) {
        final AdaptiveControlGoal goal = controlGoals.get(bossId);
        final Mob mob = bossMobs.get(bossId);
        if (goal == null || mob == null) {
            return;
        }
        final BossArchetype archetype = ctx.adaptation().getCurrentArchetype();
        final BossBehavior next = buildBehavior(archetype, mob);
        final BossBehavior current = goal.getCurrentBehavior();

        if (current == null || !archetype.equals(getCurrentArchetypeOf(current))) {
            goal.setBehavior(next);
            if (config.debugLogging.get()) {
                LOGGER.debug("[ADAPT] {} \u2192 {} | melee={}% ranged={}% magic={}%",
                        shortId(bossId),
                        archetype,
                        String.format("%.0f", pct(ctx.party().getMeleeWeight(), ctx.party())),
                        String.format("%.0f", pct(ctx.party().getRangedWeight(), ctx.party())),
                        String.format("%.0f", pct(ctx.party().getMagicWeight(), ctx.party())));
            }
        }
    }

    /**
     * Removes all state for a boss (call on death or despawn).
     *
     * @param bossId the UUID of the boss being removed
     */
    public void removeBoss(@NotNull final UUID bossId) {
        controlGoals.remove(bossId);
        bossMobs.remove(bossId);
    }

    /**
     * Returns the control goal for a boss, or {@code null} if not tracked.
     *
     * @param bossId the UUID to look up
     * @return the goal, or {@code null}
     */
    @Nullable
    public AdaptiveControlGoal getControlGoal(@NotNull final UUID bossId) {
        return controlGoals.get(bossId);
    }

    /**
     * Forces an archetype immediately, bypassing hysteresis (used by debug commands).
     *
     * @param bossId    the UUID of the target boss
     * @param archetype the archetype to force
     */
    public void forceArchetype(
            @NotNull final UUID bossId,
            @NotNull final BossArchetype archetype) {
        final AdaptiveControlGoal goal = controlGoals.get(bossId);
        final Mob mob = bossMobs.get(bossId);
        if (goal == null || mob == null) {
            return;
        }
        goal.setBehavior(buildBehavior(archetype, mob));
        if (config.debugLogging.get()) {
            LOGGER.debug("[ADAPT] {} FORCED → {}", shortId(bossId), archetype);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @NotNull
    private static BossBehavior buildBehavior(
            @NotNull final BossArchetype archetype,
            @NotNull final Mob mob) {
        return switch (archetype) {
            case KITER         -> new KiteBehavior(mob);
            case AGGRESSOR     -> new RushBehavior(mob);
            case UNPREDICTABLE -> new UnpredictableBehavior(mob);
            case BALANCED      -> new BalancedBehavior(mob);
        };
    }

    /**
     * Identifies which archetype a behavior instance represents.
     * Returns {@code null} for unknown/external behaviors.
     */
    @Nullable
    private static BossArchetype getCurrentArchetypeOf(@NotNull final BossBehavior behavior) {
        if (behavior instanceof KiteBehavior) {
            return BossArchetype.KITER;
        }
        if (behavior instanceof RushBehavior) {
            return BossArchetype.AGGRESSOR;
        }
        if (behavior instanceof UnpredictableBehavior) {
            return BossArchetype.UNPREDICTABLE;
        }
        if (behavior instanceof BalancedBehavior) {
            return BossArchetype.BALANCED;
        }
        return null;
    }

    private static double pct(
            final double weight,
            @NotNull final com.adaptiveboss.tracking.PartyProfile party) {
        final double total = party.getMeleeWeight() + party.getRangedWeight() + party.getMagicWeight();
        return total > 0 ? (weight / total) * 100.0 : 0.0;
    }

    /**
     * Returns the first 8 characters of a UUID string for compact log output.
     *
     * @param id the UUID to shorten
     * @return 8-char prefix
     */
    @NotNull
    private static String shortId(@NotNull final UUID id) {
        return id.toString().substring(0, 8);
    }

    // ── Inner helper needed by goal selector WrappedGoal scan ────────────────

    /**
     * Finds the {@link AdaptiveControlGoal} on a mob's goal selector if it was
     * added by this engine. Used for goal-level introspection in tests / commands.
     *
     * @param mob the mob to scan
     * @return the goal, or {@code null} if not found
     */
    @Nullable
    public static AdaptiveControlGoal findGoalOn(@NotNull final Mob mob) {
        for (final WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof AdaptiveControlGoal acg) {
                return acg;
            }
        }
        return null;
    }
}
