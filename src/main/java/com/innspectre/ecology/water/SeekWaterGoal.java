package com.innspectre.ecology.water;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;

import java.util.EnumSet;

public class SeekWaterGoal extends Goal {

    private final PathfinderMob animal;

    private final WaterSeekingState seekingState;
    private final WaterCooldowns cooldowns;

    private static final int SEARCH_RADIUS = 16;
    private static final long SEARCH_RETRY_DELAY = 100L;

    private long nextSearchTime;

    public SeekWaterGoal(PathfinderMob animal) {
        this.animal = animal;

        this.seekingState = new WaterSeekingState();
        this.cooldowns = new WaterCooldowns();

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private void findWaterDestination() {
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
        seekingState.stopDrinking();

        findWaterDestination();

        if (seekingState.getTarget() == null) {
            nextSearchTime = animal.level().getGameTime()
                    + SEARCH_RETRY_DELAY;
        }
    }

    @Override
    public void stop() {
        seekingState.clearTarget();
        seekingState.stopDrinking();
        animal.getNavigation().stop();
    }

    @Override
    public void tick() {
        DrinkingPosition target = seekingState.getTarget();

        if (target == null) {
            return;
        }

        double distanceSqr = animal.distanceToSqr(
        target.position().getX() + 0.5,
        target.position().getY(),
        target.position().getZ() + 0.5
        );

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

        if (distanceSqr <= 2.25) {
            if (!animal.level().getBlockState(target.waterSource()).is(net.minecraft.world.level.block.Blocks.WATER)) {
                seekingState.stopDrinking();
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
        && animal.level().getGameTime() >= seekingState.getNextDrinkDecisionTime()) {
                int thirst = NeedManager.get(animal, NeedType.THIRST);
                int drinkAmount = DrinkingCalculator.calculateDrinkAmount(thirst);

                if (drinkAmount > 0) {
                    seekingState.startDrinking(drinkAmount);

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
                        seekingState.stopDrinking();

                        seekingState.setNextDrinkDecisionTime(
                                currentTime + 200L
                        );
                    }
                }
            }

            return;
        }

        if (animal.getNavigation().isDone()) {
            animal.getNavigation().moveTo(
                    target.position().getX() + 0.5,
                    target.position().getY(),
                    target.position().getZ() + 0.5,
                    1.0
            );
        }
    }
}