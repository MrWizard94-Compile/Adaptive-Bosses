package com.adaptiveboss.adaptation;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies a soft bias to the Ender Dragon's phase-selection logic based on
 * the current party archetype.
 *
 * <p>This helper does <em>not</em> force phase changes mid-animation. It nudges
 * the dragon probabilistically only when the current phase is not locked
 * (landing, sitting, dying, etc.). The dragon still behaves naturally — it is
 * just statistically more likely to charge when facing a ranged party.
 *
 * <p>Called from {@code EnderDragonMixin.aiStep()} after every vanilla AI step.
 *
 * <p><b>API note:</b> Phase constants ({@code EnderDragonPhase.HOLDING_PATTERN},
 * {@code EnderDragonPhase.CHARGING_PLAYER}, etc.) are static final fields on the
 * {@code EnderDragonPhase} class in {@code net.minecraft.world.entity.boss.enderdragon.phases}.
 */
public final class DragonPhaseApplier {

    /** Probability boost applied when nudging toward a target phase (0–1). */
    private static final float NUDGE_CHANCE = 0.30f;

    private DragonPhaseApplier() {
        // utility class
    }

    /**
     * Evaluates whether a phase nudge should be applied this tick and, if so,
     * suggests a phase transition aligned with the current archetype.
     *
     * <p>A nudge is skipped when:
     * <ul>
     *   <li>The archetype is {@code BALANCED} or {@code UNPREDICTABLE}</li>
     *   <li>The dragon is in a phase that locks animation</li>
     *   <li>A random roll fails the {@value #NUDGE_CHANCE} probability check</li>
     * </ul>
     *
     * @param dragon    the Ender Dragon entity
     * @param archetype the current adaptation archetype
     */
    public static void applyBias(
            @NotNull final EnderDragon dragon,
            @NotNull final BossArchetype archetype) {
        if (archetype == BossArchetype.BALANCED || archetype == BossArchetype.UNPREDICTABLE) {
            return;
        }

        final var phaseManager = dragon.getPhaseManager();
        final EnderDragonPhase currentType = phaseManager.getCurrentPhase().getPhase();

        // Do not interrupt phases that lock animation
        if (isLockedPhase(currentType)) {
            return;
        }

        if (dragon.getRandom().nextFloat() >= NUDGE_CHANCE) {
            return;
        }

        final EnderDragonPhase target = selectTarget(archetype);
        if (target != null && target != currentType) {
            phaseManager.setPhase(target);
        }
    }

    @Nullable
    private static EnderDragonPhase selectTarget(@NotNull final BossArchetype archetype) {
        return switch (archetype) {
            case AGGRESSOR -> EnderDragonPhase.CHARGING_PLAYER;
            case KITER     -> EnderDragonPhase.HOLDING_PATTERN;
            default        -> null;
        };
    }

    /** Returns {@code true} for phases that lock the dragon's animation. */
    private static boolean isLockedPhase(@NotNull final EnderDragonPhase phase) {
        return phase == EnderDragonPhase.DYING
            || phase == EnderDragonPhase.SITTING_SCANNING
            || phase == EnderDragonPhase.SITTING_ATTACKING
            || phase == EnderDragonPhase.SITTING_FLAMING
            || phase == EnderDragonPhase.LANDING_APPROACH
            || phase == EnderDragonPhase.LANDING
            || phase == EnderDragonPhase.TAKEOFF;
    }
}
