package com.adaptiveboss.registry;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * Public API for registering boss entity types with the AdaptiveBoss system.
 *
 * <p>Third-party mods can call {@link #registerBoss(EntityType)} during their
 * mod initialisation to opt their bosses into the adaptation system. Wither and
 * Ender Dragon are registered automatically by this mod.
 *
 * <p>Registration is additive and thread-safe. Registering the same entity type
 * more than once is harmless.
 */
public final class AdaptiveBossRegistry {

    private static final Set<EntityType<?>> REGISTERED =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AdaptiveBossRegistry() {
        // utility class
    }

    /**
     * Registers an entity type so that instances of it are tracked and adapted
     * by the AdaptiveBoss system.
     *
     * @param entityType the entity type to register; must not be {@code null}
     */
    public static void registerBoss(@NotNull final EntityType<?> entityType) {
        REGISTERED.add(entityType);
    }

    /**
     * Returns {@code true} if the given entity type has been registered.
     *
     * @param entityType the entity type to check
     * @return {@code true} if registered
     */
    public static boolean isRegistered(@NotNull final EntityType<?> entityType) {
        return REGISTERED.contains(entityType);
    }

    /**
     * Returns an unmodifiable view of all currently registered entity types.
     *
     * @return unmodifiable set of registered entity types
     */
    @NotNull
    public static Set<EntityType<?>> getRegistered() {
        return Collections.unmodifiableSet(REGISTERED);
    }

    /** Clears all registrations — intended for use in tests only. */
    static void clearForTesting() {
        REGISTERED.clear();
    }
}
