package com.adaptiveboss.registry;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reflection-based heuristic for automatically detecting boss entities.
 *
 * <p>Most boss entities (vanilla and modded) hold a {@link ServerBossEvent}
 * field in their class or one of its superclasses to manage the boss health bar.
 * This helper scans the class hierarchy for such a field and caches the result
 * per entity class to avoid repeated reflection overhead.
 *
 * <p>This detection runs once per unique entity class encountered in the world.
 * The result is cached indefinitely in a {@link ConcurrentHashMap} and is safe
 * for concurrent server tick access.
 */
public final class BossDetectionHelper {

    private static final ConcurrentHashMap<Class<?>, Boolean> CACHE =
            new ConcurrentHashMap<>();

    private BossDetectionHelper() {
        // utility class
    }

    /**
     * Returns {@code true} if the entity's class (or any superclass) declares a
     * field of type {@link ServerBossEvent}, indicating it is likely a boss.
     *
     * @param entity the entity to inspect
     * @return {@code true} if a {@link ServerBossEvent} field is found in the hierarchy
     */
    public static boolean looksLikeBoss(@NotNull final LivingEntity entity) {
        return CACHE.computeIfAbsent(entity.getClass(), BossDetectionHelper::scanForBossEvent);
    }

    /**
     * Variant that accepts an entity class directly instead of a live entity.
     * Used by tests to avoid constructing MC entity objects.
     *
     * @param entityClass the concrete entity class to inspect
     * @return {@code true} if a {@link ServerBossEvent} field is found
     */
    static boolean looksLikeBossClass(@NotNull final Class<?> entityClass) {
        return CACHE.computeIfAbsent(entityClass, BossDetectionHelper::scanForBossEvent);
    }

    /**
     * Returns the cached result for an entity class, or {@code null} if not yet scanned.
     * Exposed for testing — production code should call {@link #looksLikeBoss(LivingEntity)}.
     *
     * @param entityClass the entity class to query
     * @return cached result, or {@code null} if not yet evaluated
     */
    @Nullable
    static Boolean getCached(@NotNull final Class<?> entityClass) {
        return CACHE.get(entityClass);
    }

    /** Clears the cache — for use in tests only. */
    static void clearCacheForTesting() {
        CACHE.clear();
    }

    private static boolean scanForBossEvent(@NotNull final Class<?> startClass) {
        Class<?> cls = startClass;
        while (cls != null && cls != Object.class) {
            for (final Field field : cls.getDeclaredFields()) {
                if (ServerBossEvent.class.isAssignableFrom(field.getType())) {
                    return true;
                }
            }
            cls = cls.getSuperclass();
        }
        return false;
    }
}
