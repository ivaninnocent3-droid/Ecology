package com.innspectre.ecology.temperament;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

public final class TemperamentGoals {

    private TemperamentGoals() {
    }

    public static void addRunningAvoidanceGoal(
            PathfinderMob animal,
            GoalSelector goalSelector
    ) {
        if (!AnimalTemperament.supports(animal)) {
            return;
        }

        goalSelector.addGoal(
                1,
                new FleeGoal(animal, 1.4)
        );
    }
}