package com.adaptiveboss.goal;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single permanent orchestrator goal that lives on a boss for the duration of
 * its fight. It holds a {@link BossBehavior} delegate and swaps it when the
 * boss's archetype changes.
 *
 * <p>This goal is added once during boss spawn and is <em>never removed</em>.
 * Swapping behaviors is done by calling {@link #setBehavior(BossBehavior)}, which
 * invokes the graceful transition hooks before committing the change.
 *
 * <p>Priority 1 keeps the behavior running concurrently with lower-priority vanilla
 * goals that handle pathing and targeting, so the boss does not lose basic tracking.
 */
public final class AdaptiveControlGoal extends Goal implements AdaptiveGoalMarker {

    @SuppressWarnings("unused")
    private final Mob boss;

    @Nullable
    private BossBehavior current;

    /**
     * Creates a new control goal for the given boss.
     *
     * @param boss the boss entity that owns this goal
     */
    public AdaptiveControlGoal(@NotNull final Mob boss) {
        this.boss    = boss;
        this.current = null;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public void start() {
        if (current != null) {
            current.start();
        }
    }

    @Override
    public void tick() {
        if (current != null) {
            current.tick();
        }
    }

    @Override
    public void stop() {
        if (current != null) {
            current.stop();
        }
    }

    /**
     * Swaps the active behavior, invoking graceful transition hooks on both the
     * outgoing and incoming behavior before committing the change.
     *
     * @param next the behavior to activate; must not be {@code null}
     */
    public void setBehavior(@NotNull final BossBehavior next) {
        if (current != null) {
            current.onSwitchFrom(next);
            current.stop();
        }
        next.onSwitchTo(current != null ? current : new BalancedBehavior(boss));
        current = next;
        current.start();
    }

    /**
     * Returns the currently active behavior, or {@code null} if none has been set.
     *
     * @return the active {@link BossBehavior}, or {@code null}
     */
    @Nullable
    public BossBehavior getCurrentBehavior() {
        return current;
    }
}
