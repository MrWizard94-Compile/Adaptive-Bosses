package com.adaptiveboss.goal;

import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.NotNull;

/**
 * Secondary behavior for the {@code AGGRESSOR} archetype.
 *
 * <p>While the boss is rushing to close the gap ({@link RushBehavior}), this
 * behavior suppresses the boss's own ranged attack cooldown, preventing it from
 * pausing mid-rush to fire projectiles that would let a ranged party kite safely.
 *
 * <p>This is achieved by artificially resetting the attack interval so the
 * ranged-attack threshold is never reached while in AGGRESSOR mode. The vanilla
 * attack goal reads this counter, so suppression is seamless.
 */
public final class ShieldBehavior implements BossBehavior {

    /** Value set on the ranged attack cooldown to prevent firing. */
    private static final int SUPPRESSED_COOLDOWN = 20;

    private final Mob boss;

    /**
     * Creates a shield (ranged-suppression) behavior.
     *
     * @param boss the owning boss entity
     */
    public ShieldBehavior(@NotNull final Mob boss) {
        this.boss = boss;
    }

    @Override
    public void start() {
        suppressRangedCooldown();
    }

    @Override
    public void tick() {
        suppressRangedCooldown();
    }

    @Override
    public void stop() {
        // Release suppression; vanilla cooldown resumes naturally.
    }

    private void suppressRangedCooldown() {
        // Continuously reset the no-action timer so vanilla ranged-attack goals
        // never see an idle window large enough to trigger a volley.
        boss.setNoActionTime(0);
    }
}
