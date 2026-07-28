package com.adaptiveboss.goal;

import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Behavior for the {@code UNPREDICTABLE} archetype — activated when the party
 * uses a balanced mix of melee and ranged attacks.
 *
 * <p>Every {@value #SWITCH_INTERVAL} ticks the behavior randomly alternates between
 * {@link KiteBehavior} and {@link RushBehavior}, preventing the party from adapting
 * to a predictable pattern.
 */
public final class UnpredictableBehavior implements BossBehavior {

    /** Ticks between random behavior switches. */
    private static final int SWITCH_INTERVAL = 40;

    private final Mob boss;
    private int ticksSinceSwitch;
    @Nullable
    private BossBehavior activeSub;

    /**
     * Creates an unpredictable behavior for the given boss.
     *
     * @param boss the owning boss entity
     */
    public UnpredictableBehavior(@NotNull final Mob boss) {
        this.boss            = boss;
        this.ticksSinceSwitch = 0;
        this.activeSub       = null;
    }

    @Override
    public void start() {
        ticksSinceSwitch = 0;
        activeSub = pickRandom();
        activeSub.start();
    }

    @Override
    public void tick() {
        ticksSinceSwitch++;
        if (ticksSinceSwitch >= SWITCH_INTERVAL) {
            switchSub();
            ticksSinceSwitch = 0;
        }
        if (activeSub != null) {
            activeSub.tick();
        }
    }

    @Override
    public void stop() {
        if (activeSub != null) {
            activeSub.stop();
            activeSub = null;
        }
        ticksSinceSwitch = 0;
    }

    private void switchSub() {
        final BossBehavior next = pickRandom();
        if (activeSub != null) {
            activeSub.stop();
        }
        activeSub = next;
        activeSub.start();
    }

    @NotNull
    private BossBehavior pickRandom() {
        return boss.getRandom().nextBoolean()
                ? new KiteBehavior(boss)
                : new RushBehavior(boss);
    }

    /** Returns the active sub-behavior (exposed for testing). */
    @Nullable
    BossBehavior getActiveSub() {
        return activeSub;
    }
}
