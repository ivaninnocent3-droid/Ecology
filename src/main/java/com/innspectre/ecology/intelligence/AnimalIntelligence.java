package com.innspectre.ecology.intelligence;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.chicken.Chicken;

public enum AnimalIntelligence {

    CHICKEN(1),
    SHEEP(2),
    COW(2),
    PIG(3),
    WOLF(5);

    private final int learningRate;

    AnimalIntelligence(int learningRate) {
        this.learningRate = learningRate;
    }

    public int learningRate() {
        return learningRate;
    }

    public static AnimalIntelligence forEntity(Entity entity) {
        if (entity instanceof Chicken) return CHICKEN;
        if (entity instanceof Sheep) return SHEEP;
        if (entity instanceof Cow) return COW;
        if (entity instanceof Pig) return PIG;
        if (entity instanceof Wolf) return WOLF;

        return SHEEP; // Temporary default for unsupported animals
    }
}