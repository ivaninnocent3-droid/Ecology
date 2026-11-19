
package com.innspectre.ecology.food;

import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AnimalDietRegistry {

    private static final Map<EntityType<?>, AnimalDiet> DIETS = new HashMap<>();

    public static void register(EntityType<?> entityType, AnimalDiet diet) {
        DIETS.put(entityType, diet);
    }

    public static Optional<AnimalDiet> getDiet(EntityType<?> entityType) {
        return Optional.ofNullable(DIETS.get(entityType));
    }
}
