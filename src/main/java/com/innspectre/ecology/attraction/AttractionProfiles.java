package com.innspectre.ecology.attraction;

import java.util.Map;
import java.util.Optional;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

/**
 * The species that can be attracted to players.
 * A species with no entry here does not use the attraction system.
 */
public final class AttractionProfiles {

    private static final Map<EntityType<?>, AttractionProfile> PROFILES = Map.of(
            EntityTypes.SHEEP, AttractionProfile.sheep()
    );

    private AttractionProfiles() {
    }

    public static Optional<AttractionProfile> get(EntityType<?> type) {
        return Optional.ofNullable(PROFILES.get(type));
    }
}