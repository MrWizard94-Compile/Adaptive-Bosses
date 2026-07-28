package com.adaptiveboss.mixin;

import com.adaptiveboss.AdaptiveBossMod;
import com.adaptiveboss.adaptation.BossArchetype;
import com.adaptiveboss.goal.AdaptiveControlGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for {@link WitherBoss} that integrates it with the AdaptiveBoss system.
 *
 * <ul>
 *   <li>{@code registerGoals TAIL} — registers the Wither with
 *       {@link com.adaptiveboss.registry.AdaptiveBossRegistry} and adds the
 *       permanent {@link AdaptiveControlGoal}.
 *   <li>{@code performRangedAttack HEAD} — cancels skull volleys when the current
 *       archetype is {@link BossArchetype#AGGRESSOR} (the boss should be rushing,
 *       not shooting).
 * </ul>
 */
@Mixin(WitherBoss.class)
public final class WitherMixin {

    /**
     * Adds the adaptive control goal after vanilla goals are registered.
     *
     * @param ci the callback info (not used)
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void onRegisterGoals(@NotNull final CallbackInfo ci) {
        final WitherBoss self = (WitherBoss) (Object) this;
        AdaptiveBossMod.getEngine().ensureControlGoalAdded(self);
    }

    /**
     * Suppresses skull volleys when the boss is in AGGRESSOR mode so it closes
     * the gap rather than stopping to shoot.
     *
     * <p>Targets {@code performRangedAttack(LivingEntity, float)} — the
     * {@link net.minecraft.world.entity.monster.RangedAttackMob} entry point
     * called by the goal — using an explicit descriptor to avoid ambiguity with
     * the private {@code performRangedAttack(int, LivingEntity)} overload.
     *
     * @param target   the intended ranged-attack target
     * @param velocity projectile velocity passed by the goal (not used here)
     * @param ci       the cancellable callback info
     */
    @Inject(
            method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onPerformRangedAttack(
            @NotNull final LivingEntity target,
            final float velocity,
            @NotNull final CallbackInfo ci) {
        final WitherBoss self = (WitherBoss) (Object) this;
        final var profile = AdaptiveBossMod.getTracker().getAdaptationProfile(self.getUUID());
        if (profile != null && profile.getCurrentArchetype() == BossArchetype.AGGRESSOR) {
            ci.cancel();
        }
    }
}
