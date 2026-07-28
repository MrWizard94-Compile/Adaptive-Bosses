package com.adaptiveboss.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.NotNull;

/**
 * Behavior for the {@code KITER} archetype — activated when the party is
 * predominantly melee fighters.
 *
 * <p>The boss attempts to maintain at least {@value #KITE_DISTANCE} blocks from
 * its nearest attacker and executes an AoE sweep every {@value #AOE_INTERVAL} ticks.
 */
public final class KiteBehavior implements BossBehavior {

    /** Minimum distance the boss tries to maintain from the closest attacker. */
    private static final double KITE_DISTANCE = 8.0;
    /** Ticks between AoE sweep attempts. */
    private static final int    AOE_INTERVAL  = 20;

    private final Mob boss;
    private int ticksSinceAoe;

    /**
     * Creates a kite behavior for the given boss.
     *
     * @param boss the owning boss entity
     */
    public KiteBehavior(@NotNull final Mob boss) {
        this.boss        = boss;
        this.ticksSinceAoe = 0;
    }

    @Override
    public void start() {
        ticksSinceAoe = 0;
    }

    @Override
    public void tick() {
        ticksSinceAoe++;
        final LivingEntity target = boss.getTarget();
        if (target == null) {
            return;
        }
        maintainDistance(target);
        if (ticksSinceAoe >= AOE_INTERVAL) {
            performAoeSweep();
            ticksSinceAoe = 0;
        }
    }

    @Override
    public void stop() {
        ticksSinceAoe = 0;
        // Let vanilla pathfinding take over naturally; no forced state to clear.
    }

    private void maintainDistance(@NotNull final LivingEntity target) {
        final double distSq = boss.distanceToSqr(target);
        if (distSq < KITE_DISTANCE * KITE_DISTANCE) {
            // Move away from the target
            final double dx = boss.getX() - target.getX();
            final double dz = boss.getZ() - target.getZ();
            final double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0.01) {
                final double tx = boss.getX() + (dx / len) * KITE_DISTANCE;
                final double tz = boss.getZ() + (dz / len) * KITE_DISTANCE;
                boss.getNavigation().moveTo(tx, boss.getY(), tz, 1.3);
            }
        }
    }

    private void performAoeSweep() {
        // Trigger a small knockback burst on all nearby living entities within 5 blocks.
        // Actual damage is deferred to server-side explosion logic to stay safe.
        boss.level().getEntitiesOfClass(
                LivingEntity.class,
                boss.getBoundingBox().inflate(5.0),
                e -> e != boss && e.isAlive()
        ).forEach(e -> e.knockback(1.5, boss.getX() - e.getX(), boss.getZ() - e.getZ()));
    }

    /** Returns the current AoE tick counter (exposed for testing). */
    int getTicksSinceAoe() {
        return ticksSinceAoe;
    }

    /** Returns the configured kite distance (exposed for testing). */
    static double kiteDistance() {
        return KITE_DISTANCE;
    }
}
