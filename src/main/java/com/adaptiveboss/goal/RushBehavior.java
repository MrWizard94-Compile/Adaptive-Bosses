package com.adaptiveboss.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.NotNull;

/**
 * Primary behavior for the {@code AGGRESSOR} archetype — activated when the party
 * is predominantly ranged fighters.
 *
 * <p>The boss sprints aggressively toward its nearest attacker, attempting to close
 * the gap and force melee engagement. Pairs with {@link ShieldBehavior} (added as a
 * secondary by the engine) to suppress the boss's own ranged attacks.
 */
public final class RushBehavior implements BossBehavior {

    /** Sprint speed multiplier applied while rushing. */
    private static final double RUSH_SPEED = 1.5;
    /** Distance at which the boss considers itself "in melee" and stops rushing. */
    private static final double MELEE_RANGE = 3.0;

    private final Mob boss;

    /**
     * Creates a rush behavior for the given boss.
     *
     * @param boss the owning boss entity
     */
    public RushBehavior(@NotNull final Mob boss) {
        this.boss = boss;
    }

    @Override
    public void start() {
        // Nothing to set up; path is computed each tick.
    }

    @Override
    public void tick() {
        final LivingEntity target = boss.getTarget();
        if (target == null) {
            return;
        }
        if (boss.distanceToSqr(target) > MELEE_RANGE * MELEE_RANGE) {
            boss.getNavigation().moveTo(target, RUSH_SPEED);
        }
    }

    @Override
    public void stop() {
        // Stop any in-progress rush path so the next behavior starts cleanly.
        boss.getNavigation().stop();
    }

    /** Returns the melee range threshold (exposed for testing). */
    static double meleeRange() {
        return MELEE_RANGE;
    }
}
