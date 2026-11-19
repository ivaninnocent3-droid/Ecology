
package com.innspectre.ecology.food;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public interface FoodSource {

    Optional<FoodTarget> findFood(LivingEntity animal, int searchRadius);

    interface FoodTarget {

        Vec3 position();

        boolean consume(LivingEntity animal);
    }
}
