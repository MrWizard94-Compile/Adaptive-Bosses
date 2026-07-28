package com.adaptiveboss.registry;

import java.lang.reflect.Constructor;
import net.minecraft.server.level.ServerBossEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BossDetectionHelper}.
 *
 * <p>Uses plain Java stub classes (no MC runtime required). The package-private
 * {@code looksLikeBossClass(Class)} overload exercises the detection logic
 * without constructing Minecraft entities.
 */
class BossDetectionHelperTest {

    @AfterEach
    void clearCache() {
        BossDetectionHelper.clearCacheForTesting();
    }

    // ── Stub classes (plain Java, no MC deps in constructor) ─────────────────

    /** A class that simulates a boss — it has a {@link ServerBossEvent} field. */
    @SuppressWarnings("unused")
    static final class FakeBossEntity {

        private final ServerBossEvent bossEvent = null;
    }

    /** A class that simulates a non-boss — no {@link ServerBossEvent} field. */
    static final class FakeCreeperEntity {
        // No ServerBossEvent field
    }

    /** Base class with the field — used to test inheritance scanning. */
    @SuppressWarnings("unused")
    static class FakeBossBase {

        private final ServerBossEvent bossEvent = null;
    }

    /** Subclass that inherits the field from {@link FakeBossBase}. */
    static final class FakeBossSubclass extends FakeBossBase {
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void looksLikeBossClass_entityWithServerBossEventField_returnsTrue() {
        assertTrue(BossDetectionHelper.looksLikeBossClass(FakeBossEntity.class));
    }

    @Test
    void looksLikeBossClass_entityWithoutBossEventField_returnsFalse() {
        assertFalse(BossDetectionHelper.looksLikeBossClass(FakeCreeperEntity.class));
    }

    @Test
    void looksLikeBossClass_inheritedFieldDetected() {
        assertTrue(BossDetectionHelper.looksLikeBossClass(FakeBossSubclass.class));
    }

    @Test
    void looksLikeBossClass_cachePreventsDuplicateScan() {
        // First call — cache is empty, scan happens
        assertNull(BossDetectionHelper.getCached(FakeBossEntity.class));
        assertTrue(BossDetectionHelper.looksLikeBossClass(FakeBossEntity.class));

        // Cache must now hold the result
        assertNotNull(BossDetectionHelper.getCached(FakeBossEntity.class));
        assertTrue(BossDetectionHelper.getCached(FakeBossEntity.class));

        // Second call returns cached value without re-scanning
        assertTrue(BossDetectionHelper.looksLikeBossClass(FakeBossEntity.class));
    }

    @Test
    void getCached_nullBeforeScan() {
        assertNull(BossDetectionHelper.getCached(FakeBossEntity.class));
    }

    @Test
    void looksLikeBossClass_objectClassReturnsFalse() {
        // Walking up to Object.class must not throw and must return false
        assertFalse(BossDetectionHelper.looksLikeBossClass(Object.class));
    }

    @Test
    void privateConstructor_coverageHelper() throws Exception {
        final Constructor<BossDetectionHelper> ctor =
                BossDetectionHelper.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
