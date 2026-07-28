package com.adaptiveboss.mixin;

import com.adaptiveboss.AdaptiveBossMod;
import com.adaptiveboss.adaptation.BossArchetype;
import com.adaptiveboss.adaptation.DragonPhaseApplier;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for {@link EnderDragon} that applies soft phase biasing via
 * {@link DragonPhaseApplier} after each {@code aiStep()} call.
 *
 * <p>Injection at {@code TAIL} ensures that vanilla AI — including phase transitions
 * triggered by crystal state, gateway logic, and navigation — runs fully before
 * the bias check. This prevents the bias from interfering with animation locks.
 */
@Mixin(EnderDragon.class)
public final class EnderDragonMixin {

    /**
     * Applies the phase bias after vanilla AI has completed its step.
     *
     * @param ci the callback info (not used)
     */
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(@NotNull final CallbackInfo ci) {
        final EnderDragon self = (EnderDragon) (Object) this;

        // Only apply on the server side
        if (self.level().isClientSide()) {
            return;
        }

        final var profile = AdaptiveBossMod.getTracker().getAdaptationProfile(self.getUUID());
        final BossArchetype archetype = profile != null
                ? profile.getCurrentArchetype()
                : BossArchetype.BALANCED;

        DragonPhaseApplier.applyBias(self, archetype);
    }
}
