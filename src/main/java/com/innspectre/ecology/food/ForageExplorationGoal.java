
package com.innspectre.ecology.food;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import com.innspectre.ecology.shelter.ShelterManager;

public class ForageExplorationGoal extends Goal {

    private static final int CHECK_DELAY = 20;
    private static final double ARRIVAL_DISTANCE_SQR = 4.0;

    private final PathfinderMob animal;
    private final double speed;

    private BlockPos target;
    private boolean pathFound;

    public ForageExplorationGoal(
            PathfinderMob animal,
            double speed
    ) {
        this.animal = animal;
        this.speed = speed;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {

        if (animal.getRandom().nextInt(
                reducedTickDelay(CHECK_DELAY)
        ) != 0) {
            return false;
        }

        if (!(animal.level() instanceof ServerLevel level)) {
            return false;
        }

        if (ShelterManager.isShelterSearchActive(animal)) {
            return false;
        }

        target = ForageExplorationManager.getDestination(
                level,
                animal
        ).orElse(null);

        if (target == null) {
            return false;
        }

        return animal.blockPosition().distSqr(target)
                > ARRIVAL_DISTANCE_SQR;
    }

    @Override
    public void start() {
        pathFound = animal.getNavigation().moveTo(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                speed
        );
    }

    @Override
    public boolean canContinueToUse() {
        return pathFound
                && !animal.getNavigation().isDone()
                && ForageExplorationManager.isExploring(animal)
                && !ShelterManager.isShelterSearchActive(animal);
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();

        target = null;
        pathFound = false;
    }
}
