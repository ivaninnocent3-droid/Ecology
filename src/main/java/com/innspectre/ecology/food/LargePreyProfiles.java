package com.innspectre.ecology.food;

import java.util.Map;
import java.util.Optional;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

public final class LargePreyProfiles {

    private static final Map<EntityType<?>, PreyRemainsType> PROFILES = Map.of(
            EntityTypes.SHEEP, PreyRemainsType.WOOL,
            EntityTypes.COW, PreyRemainsType.LEATHER,
            EntityTypes.CHICKEN, PreyRemainsType.FEATHERS,
            EntityTypes.PIG, PreyRemainsType.NONE,
            EntityTypes.HORSE, PreyRemainsType.LEATHER,
            EntityTypes.RABBIT, PreyRemainsType.NONE,
            EntityTypes.FOX, PreyRemainsType.NONE
    );

    private LargePreyProfiles() {
    }

    public static Optional<PreyRemainsType> get(EntityType<?> type) {
        return Optional.ofNullable(PROFILES.get(type));
    }

    public static boolean isLargePrey(EntityType<?> type) {
        return PROFILES.containsKey(type);
    }
}