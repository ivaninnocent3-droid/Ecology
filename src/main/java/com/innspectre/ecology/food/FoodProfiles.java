
package com.innspectre.ecology.food;

import java.util.Optional;
import java.util.Map;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

public final class FoodProfiles {

    private static final Map<EntityType<?>, FoodProfile> PROFILES = Map.of(
        EntityTypes.SHEEP, FoodProfile.sheep(),
        EntityTypes.COW, FoodProfile.cow(),
        EntityTypes.CHICKEN, FoodProfile.chicken(),
        EntityTypes.PIG, FoodProfile.pig(),
        EntityTypes.WOLF, FoodProfile.wolf()
    );

    private FoodProfiles() {
    }

    public static Optional<FoodProfile> get(EntityType<?> type) {
        return Optional.ofNullable(PROFILES.get(type));
    }
}
