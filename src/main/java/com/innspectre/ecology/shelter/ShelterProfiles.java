package com.innspectre.ecology.shelter;

import java.util.Map;
import java.util.Optional;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

public final class ShelterProfiles {

    private static final ShelterProfile DEFAULT =
            new ShelterProfile(12, 5, 3, 4);

    private static final Map<EntityType<?>, ShelterProfile> PROFILES = Map.of(
            EntityTypes.SHEEP, DEFAULT,
            EntityTypes.COW, DEFAULT,
            EntityTypes.PIG, DEFAULT,
            EntityTypes.CHICKEN, DEFAULT
    );

    private ShelterProfiles() {
    }

    public static Optional<ShelterProfile> get(EntityType<?> type) {
        return Optional.ofNullable(PROFILES.get(type));
    }
}