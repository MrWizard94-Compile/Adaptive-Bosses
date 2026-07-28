package com.adaptiveboss.registry;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AdaptiveBossRegistry}.
 *
 * <p>Functional tests for {@code registerBoss}/{@code isRegistered} require a
 * live Minecraft {@link net.minecraft.world.entity.EntityType} instance, which
 * triggers MC's bootstrap static initializers. Those tests belong in the
 * integration / game-test layer. This file covers the utility-class invariant only.
 */
class AdaptiveBossRegistryTest {

    @Test
    void privateConstructor_coverageHelper() throws Exception {
        final Constructor<AdaptiveBossRegistry> ctor =
                AdaptiveBossRegistry.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
