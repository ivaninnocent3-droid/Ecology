
package com.innspectre.ecology.water;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;

import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;
import com.innspectre.ecology.animation.AnimalActionManager;

import java.util.EnumSet;
import java.util.List;

public class SeekWaterGoal extends Goal {

    private final PathfinderMob animal;

    private final WaterSeekingState seekingState;
    private final WaterCooldowns cooldowns;

    private static final int SEARCH_RADIUS = 16;
    private static final long SEARCH_RETRY_DELAY = 100L;
    private static final long SHARED_WATER_CHECK_INTERVAL = 100L;

    private long nextSearchTime;
    private long nextSharedWaterCheckTime;

    public SeekWaterGoal(PathfinderMob animal) {
        this.animal = animal;

        this.seekingState = new WaterSeekingState();
        this.cooldowns = new WaterCooldowns();

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * First tries water known to the herd.
     * If that isn't usable, performs the normal local search.
     */
    private void findWaterDestination() {
        if (findKnownWaterDestination()) {
            return;
        }

        findLocalWaterDestination();
    }

    private boolean findKnownWaterDestination() {
        BlockPos knownWater = HerdWaterMemory
                .getKnownWater(animal)
                .orElse(null);

        if (knownWater == null) {
            return false;
        }

        long currentTime = animal.level().getGameTime();

        if (cooldowns.isOnCooldown(knownWater, currentTime)) {
            return false;
        }

        List<DrinkingPosition> positions =
                WaterFinder.findAvailableDrinkingPositionsAtSource(
                        animal,
                        knownWater
                );

        DrinkingPosition target = WaterFinder.chooseDrinkingPosition(
                positions,
                animal.blockPosition(),
                animal.getRandom()
        );

        if (target == null) {
            return false;
        }

        seekingState.setTarget(target);
        return true;
    }

    private void findLocalWaterDestination() {
        var positions = WaterFinder.findAvailableDrinkingPositions(
                animal,
                animal.blockPosition(),
                SEARCH_RADIUS,
                cooldowns
        );

        DrinkingPosition target = WaterFinder.chooseDrinkingPosition(
                positions,
                animal.blockPosition(),
                animal.getRandom()
        );

        seekingState.setTarget(target);
    }

    @Override
    public boolean canUse() {
        int thirst = NeedManager.get(animal, NeedType.THIRST);

        return thirst <= 60
                && animal.level().getGameTime() >= nextSearchTime;
    }

    @Override
    public boolean canContinueToUse() {
        int thirst = NeedManager.get(animal, NeedType.THIRST);

        return (thirst <= 60 || seekingState.isDrinking())
                && seekingState.getTarget() != null;
    }

    @Override
    public void start() {
        seekingState.clearTarget();
        stopDrinking();

        nextSharedWaterCheckTime = 0;

        findWaterDestination();

        if (seekingState.getTarget() == null) {
            nextSearchTime = animal.level().getGameTime()
                    + SEARCH_RETRY_DELAY;
        }
    }

    @Override
    public void stop() {
        seekingState.clearTarget();
        stopDrinking();

        animal.getNavigation().stop();
    }

    /**
     * Checks periodically whether another herd member has found water.
     *
     * This also allows an animal already searching independently to change
     * course when its herd discovers a usable water source.
     */
    private void checkForSharedWater() {
        long currentTime = animal.level().getGameTime();

        if (seekingState.isDrinking()
                || currentTime < nextSharedWaterCheckTime) {
            return;
        }

        nextSharedWaterCheckTime =
                currentTime + SHARED_WATER_CHECK_INTERVAL;

        BlockPos knownWater = HerdWaterMemory
                .getKnownWater(animal)
                .orElse(null);

        if (knownWater == null) {
            return;
        }

        DrinkingPosition currentTarget = seekingState.getTarget();

        // Already travelling towards this source.
        if (currentTarget != null
                && currentTarget.waterSource().equals(knownWater)) {
            return;
        }

        if (cooldowns.isOnCooldown(knownWater, currentTime)) {
            return;
        }

        List<DrinkingPosition> positions =
                WaterFinder.findAvailableDrinkingPositionsAtSource(
                        animal,
                        knownWater
                );

        DrinkingPosition sharedTarget = WaterFinder.chooseDrinkingPosition(
                positions,
                animal.blockPosition(),
                animal.getRandom()
        );

        if (sharedTarget != null) {
            seekingState.setTarget(sharedTarget);
            animal.getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        checkForSharedWater();

        DrinkingPosition target = seekingState.getTarget();

        if (target == null) {
            return;
        }

        double distanceSqr = animal.distanceToSqr(
                target.position().getX() + 0.5,
                target.position().getY(),
                target.position().getZ() + 0.5
        );

        // Abandon water sources that the animal cannot reach.
        if (seekingState.checkProgress(
                Math.sqrt(distanceSqr),
                animal.level().getGameTime()
        )) {
            cooldowns.markUnreachable(
                    target.waterSource(),
                    animal.level().getGameTime()
            );

            seekingState.clearTarget();
            animal.getNavigation().stop();

            nextSearchTime = animal.level().getGameTime()
                    + SEARCH_RETRY_DELAY;

            return;
        }

        // Close enough to drink.
        if (distanceSqr <= 2.25) {

            // The source may have changed since the destination was selected.
            if (!animal.level().getBlockState(target.waterSource()).is(Blocks.WATER)
                    || animal.level().getFluidState(target.waterSource()).getAmount() != 8) {

                stopDrinking();
                seekingState.clearTarget();
                animal.getNavigation().stop();

                findWaterDestination();

                if (seekingState.getTarget() == null) {
                    nextSearchTime = animal.level().getGameTime()
                            + SEARCH_RETRY_DELAY;
                }

                return;
            }

            animal.getNavigation().stop();

            if (!seekingState.isDrinking()
                    && animal.level().getGameTime()
                    >= seekingState.getNextDrinkDecisionTime()) {

                int thirst = NeedManager.get(animal, NeedType.THIRST);
                int drinkAmount = DrinkingCalculator.calculateDrinkAmount(thirst);

                if (drinkAmount > 0) {

                    // The animal has successfully found water.
                    // Share that discovery with its herd.
                    HerdWaterMemory.reportWater(
                            animal,
                            target.waterSource()
                    );

                    seekingState.startDrinking(drinkAmount);
                    AnimalActionManager.startDrinking(animal);

                    seekingState.setNextDrinkPointTime(
                            animal.level().getGameTime()
                                    + DrinkingCalculator.getTicksPerDrinkPoint()
                    );
                }
            }

            if (seekingState.isDrinking()) {
                long currentTime = animal.level().getGameTime();

                if (currentTime >= seekingState.getNextDrinkPointTime()) {
                    NeedManager.change(animal, NeedType.THIRST, 1);

                    seekingState.reduceRemainingDrinkAmount(1);

                    seekingState.setNextDrinkPointTime(
                            currentTime + DrinkingCalculator.getTicksPerDrinkPoint()
                    );

                    if (seekingState.getRemainingDrinkAmount() <= 0) {
                        stopDrinking();

                        seekingState.setNextDrinkDecisionTime(
                                currentTime + 200L
                        );
                    }
                }
            }

            return;
        }

        // Continue towards the selected drinking position.
        if (animal.getNavigation().isDone()) {
            animal.getNavigation().moveTo(
                    target.position().getX() + 0.5,
                    target.position().getY(),
                    target.position().getZ() + 0.5,
                    1.0
            );
        }
    }

    private void stopDrinking() {
        if (seekingState.isDrinking()) {
            AnimalActionManager.stopDrinking(animal);
            stopDrinking();
        }
    }
}
