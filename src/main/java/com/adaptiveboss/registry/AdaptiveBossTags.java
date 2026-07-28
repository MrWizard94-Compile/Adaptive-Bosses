package com.adaptiveboss.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/** Tag keys used by the AdaptiveBoss mod. */
public final class AdaptiveBossTags {

    /**
     * Entity types in this tag are automatically tracked and adapted by the
     * AdaptiveBoss system. Modders and pack makers can add their bosses here
     * without writing any Java code.
     *
     * <p>Data path: {@code data/adaptiveboss/tags/entity_type/bosses.json}
     */
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("adaptiveboss", "bosses"));

    private AdaptiveBossTags() {
        // utility class
    }
}
