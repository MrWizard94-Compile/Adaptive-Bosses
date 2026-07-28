package com.adaptiveboss;

import com.adaptiveboss.tracking.DamageType;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DamageType#classify(DamageSource)}.
 *
 * <p>{@link DamageSource} is mocked via Mockito so no Minecraft runtime is required.
 * The {@link DamageTypeTags} constants are plain {@code TagKey} objects and resolve
 * without bootstrapping the MC registry system.
 */
class DamageTypeTest {

    @Test
    void classify_projectileSource_returnsRanged() {
        final DamageSource src = mock(DamageSource.class);
        when(src.is(DamageTypeTags.IS_PROJECTILE)).thenReturn(true);
        assertEquals(DamageType.RANGED, DamageType.classify(src));
    }

    @Test
    void classify_bypassesArmorNotProjectile_returnsMagic() {
        final DamageSource src = mock(DamageSource.class);
        when(src.is(DamageTypeTags.IS_PROJECTILE)).thenReturn(false);
        when(src.is(DamageTypeTags.BYPASSES_ARMOR)).thenReturn(true);
        assertEquals(DamageType.MAGIC, DamageType.classify(src));
    }

    @Test
    void classify_regularDamage_returnsMelee() {
        final DamageSource src = mock(DamageSource.class);
        when(src.is(DamageTypeTags.IS_PROJECTILE)).thenReturn(false);
        when(src.is(DamageTypeTags.BYPASSES_ARMOR)).thenReturn(false);
        assertEquals(DamageType.MELEE, DamageType.classify(src));
    }
}
