package com.adaptiveboss.tracking;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.NotNull;

/** Broad category of damage inflicted on a boss. */
public enum DamageType {

    /** Physical melee (sword, axe, fist). */
    MELEE,

    /** Projectile or thrown (arrow, crossbow, trident). */
    RANGED,

    /** Magic or direct (potions, magic, fall-through-armor). */
    MAGIC;

    /**
     * Classifies a {@link DamageSource} into one of the three categories.
     *
     * @param source the damage source to classify
     * @return the corresponding {@link DamageType}
     */
    @NotNull
    public static DamageType classify(@NotNull final DamageSource source) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return RANGED;
        }
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return MAGIC;
        }
        return MELEE;
    }
}
