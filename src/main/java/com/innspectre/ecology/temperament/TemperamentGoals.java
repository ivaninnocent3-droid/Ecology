
package com.innspectre.ecology.temperament;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;

public class TemperamentGoals {

    public static void addRunningAvoidanceGoal(PathfinderMob animal, GoalSelector goalSelector) {

        if (!AnimalTemperament.forEntity(animal).fleesFromRunningPlayer()) {
            return;
        }

        goalSelector.addGoal(1,
                new AvoidEntityGoal<Player>(
                        animal,
                        Player.class,
                        AnimalTemperament.forEntity(animal).avoidanceDistance(),
                        1.0,
                        1.4,
                        player -> player.isSprinting()
                )
        );
    }
}
