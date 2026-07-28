package com.adaptiveboss.goal;

import org.jetbrains.annotations.NotNull;

/**
 * Strategy interface for boss combat behaviors.
 *
 * <p>Behaviors are lightweight strategy objects held by {@link AdaptiveControlGoal}.
 * They are swapped atomically when the boss's archetype changes, providing clean
 * lifecycle hooks without touching vanilla goal selector state.
 *
 * <p>Implementations should keep minimal internal state. All heavy state
 * (pathfinding targets, attack cooldowns) must be cleaned up in {@link #stop()}
 * to prevent bleed-over when the behavior is replaced.
 */
public interface BossBehavior {

    /** Called once when this behavior becomes the active behavior. */
    void start();

    /** Called every tick while this behavior is active. */
    void tick();

    /**
     * Called once when this behavior is replaced.
     * Must reset any transient state (pathfinding, cooldowns) set during {@link #start()}.
     */
    void stop();

    /**
     * Called just before {@link #start()} when switching from another behavior.
     * Override for graceful transitions (e.g., cancel an in-progress attack).
     *
     * @param previous the behavior being replaced
     */
    default void onSwitchTo(@NotNull final BossBehavior previous) {
    }

    /**
     * Called just before {@link #stop()} when being replaced by another behavior.
     *
     * @param next the behavior that will become active after this one stops
     */
    default void onSwitchFrom(@NotNull final BossBehavior next) {
    }
}
