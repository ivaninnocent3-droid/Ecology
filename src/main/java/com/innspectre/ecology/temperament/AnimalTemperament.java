
package com.innspectre.ecology.temperament;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.chicken.Chicken;

public enum AnimalTemperament {

    CHICKEN(4, 12),
    SHEEP(3, 8),
    COW(1, 3),
    PIG(2, 5),
    WOLF(2, 5);

    private static final int RUNNING_FEAR_THRESHOLD = 3;

    private final int fearfulness;
    private final float avoidanceDistance;

    AnimalTemperament(int fearfulness, float avoidanceDistance) {
        this.fearfulness = fearfulness;
        this.avoidanceDistance = avoidanceDistance;
    }

    public int fearfulness() {
        return fearfulness;
    }

    public float avoidanceDistance() {
        return avoidanceDistance;
    }

    public boolean fleesFromRunningPlayer() {
        return fearfulness >= RUNNING_FEAR_THRESHOLD;
    }

    public static AnimalTemperament forEntity(Entity entity) {
        if (entity instanceof Chicken) return CHICKEN;
        if (entity instanceof Sheep) return SHEEP;
        if (entity instanceof Cow) return COW;
        if (entity instanceof Pig) return PIG;
        if (entity instanceof Wolf) return WOLF;

        return SHEEP;
    }
}
