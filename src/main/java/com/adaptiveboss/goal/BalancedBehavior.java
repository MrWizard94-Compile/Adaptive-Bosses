package com.adaptiveboss.goal;

import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.NotNull;

/**
 * No-op behavior used when the boss has not yet gathered enough combat data
 * ({@code BALANCED} archetype). Vanilla AI goals run uninterrupted.
 */
public final class BalancedBehavior implements BossBehavior {

    @SuppressWarnings("unused")
    private final Mob boss;

    /**
     * Creates a balanced (no-op) behavior.
     *
     * @param boss the owning boss entity
     */
    public BalancedBehavior(@NotNull final Mob boss) {
        this.boss = boss;
    }

    @Override
    public void start() {
        // No special setup; vanilla goals run normally.
    }

    @Override
    public void tick() {
        // No override; vanilla AI drives the boss.
    }

    @Override
    public void stop() {
        // Nothing to clean up.
    }
}
