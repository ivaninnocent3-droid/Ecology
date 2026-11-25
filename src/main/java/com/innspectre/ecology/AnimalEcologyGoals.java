
package com.innspectre.ecology;

import com.innspectre.ecology.attraction.AttractionGoal;
import com.innspectre.ecology.attraction.AttractionProfiles;
import com.innspectre.ecology.water.SeekWaterGoal;
import com.innspectre.ecology.water.WaterExplorationGoal;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

public final class AnimalEcologyGoals {

    private AnimalEcologyGoals() {
    }

    public static void addSharedGoals(
            PathfinderMob animal,
            GoalSelector goals
    ) {
        goals.addGoal(2, new SeekWaterGoal(animal));

        addAttractionGoal(animal, goals);

        goals.addGoal(4, new WaterExplorationGoal(animal, 1.0));
    }

    public static void addAttractionGoal(
            PathfinderMob animal,
            GoalSelector goals
    ) {
        AttractionProfiles.get(animal.getType()).ifPresent(profile ->
                goals.addGoal(3, new AttractionGoal(animal, profile))
        );
    }
}
