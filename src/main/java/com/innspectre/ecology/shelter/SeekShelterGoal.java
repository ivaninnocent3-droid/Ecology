package com.innspectre.ecology.shelter;

import java.util.EnumSet;

import com.innspectre.ecology.herd.Herd;
import com.innspectre.ecology.herd.HerdManager;
import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class SeekShelterGoal extends Goal {

    private static final int HUNGRY_THRESHOLD = 60;
    private static final int THIRSTY_THRESHOLD = 60;

    private static final double ARRIVAL_DISTANCE_SQR = 2.25;

    private static final long FAILED_PATH_RETRY = 100L;
    private static final long PATH_RETRY_DELAY = 40L;

    private final PathfinderMob animal;
    private final double speed;

    private BlockPos target;
    private boolean pathFound;

    private long nextAttemptTime;
    private long nextPathAttemptTime;

    public SeekShelterGoal(PathfinderMob animal, double speed) {
        this.animal = animal;
        this.speed = speed;

        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(animal.level() instanceof ServerLevel level)) {
            return false;
        }

        if (!ShelterManager.isShelterTime(level)
                || !hasNonUrgentNeeds()
                || level.getGameTime() < nextAttemptTime) {
            return false;
        }

        if (!ShelterManager.isShelterSearchActive(animal)) {
            return false;
        }

        ShelterTarget shelter = ShelterManager
                .getShelterTarget(animal)
                .orElse(null);

        if (shelter == null) {
            return false;
        }

        target = shelter.positionFor(animal.getUUID()).orElse(null);

        if (target == null) {
            return false;
        }

        if (shelter.roofed() && !ShelterFinder.isValidSpot(level, target)) {
            return false;
        }

        return animal.blockPosition().distSqr(target) > 2;
    }

    @Override
    public void start() {
        long currentTime = animal.level().getGameTime();

        if (distanceToTarget() <= ARRIVAL_DISTANCE_SQR) {
            pathFound = true;
            animal.getNavigation().stop();
        } else {
            pathFound = animal.getNavigation().moveTo(
                    target.getX() + 0.5,
                    target.getY(),
                    target.getZ() + 0.5,
                    speed
            );
        }

        if (!pathFound) {
            nextAttemptTime = currentTime + FAILED_PATH_RETRY;
        }

        nextPathAttemptTime = currentTime + PATH_RETRY_DELAY;
    }

    @Override
    public boolean canContinueToUse() {
        if (!pathFound || target == null) {
            return false;
        }

        if (!(animal.level() instanceof ServerLevel level)
                || !ShelterManager.isShelterTime(level)
                || !hasNonUrgentNeeds()) {
            return false;
        }

        return ShelterManager.getShelterTarget(animal)
                .flatMap(shelter ->
                        shelter.positionFor(animal.getUUID()))
                .filter(target::equals)
                .isPresent();
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }

        if (distanceToTarget() <= ARRIVAL_DISTANCE_SQR) {
            animal.getNavigation().stop();
            return;
        }

        long currentTime = animal.level().getGameTime();

        if (animal.getNavigation().isDone()
                && currentTime >= nextPathAttemptTime) {

            pathFound = animal.getNavigation().moveTo(
                    target.getX() + 0.5,
                    target.getY(),
                    target.getZ() + 0.5,
                    speed
            );

            nextPathAttemptTime = currentTime + PATH_RETRY_DELAY;

            if (!pathFound) {
                nextAttemptTime = currentTime + FAILED_PATH_RETRY;
            }
        }
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();

        target = null;
        pathFound = false;
    }

    private boolean hasNonUrgentNeeds() {
        return NeedManager.get(animal, NeedType.HUNGER)
                    > HUNGRY_THRESHOLD
                && NeedManager.get(animal, NeedType.THIRST)
                    > THIRSTY_THRESHOLD;
    }

    private double distanceToTarget() {
        return animal.distanceToSqr(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5
        );
    }
}