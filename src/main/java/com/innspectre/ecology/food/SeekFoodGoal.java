
package com.innspectre.ecology.food;

import java.util.EnumSet;
import java.util.function.Consumer;

import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;
import com.innspectre.ecology.animation.AnimalActionManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;


public class SeekFoodGoal extends Goal {

    private static final int SEARCH_RADIUS = 16;
    private static final long SEARCH_RETRY_DELAY = 100L;

    private static final double ARRIVAL_DISTANCE_SQR = 2.25;

    private static final int HUNGRY_THRESHOLD = 60;

    private static final int ROUTINE_CHECK_INTERVAL = 100;
    private static final int MIN_GRAZING_COOLDOWN = 200;
    private static final int MAX_GRAZING_COOLDOWN = 600;

    private static final int EATING_ANIMATION_DURATION = 40;
    private static final int EATING_CONSUMPTION_TICK = 4;

    private final PathfinderMob animal;
    private final Consumer<PathfinderMob> afterEating;
    private final Consumer<PathfinderMob> onEatingStarted;

    private VegetationTarget target;

    private long nextSearchTime;
    private long nextRoutineCheckTime;

    private boolean pathFound;
    private boolean arrived;
    private boolean grazed;
    private boolean eating;

    private int eatingTicks;

    public SeekFoodGoal(
            PathfinderMob animal,
            Consumer<PathfinderMob> onEatingStarted,
            Consumer<PathfinderMob> afterEating
    ) {
        this.animal = animal;
        this.onEatingStarted = onEatingStarted;
        this.afterEating = afterEating;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        long currentTime = animal.level().getGameTime();

        if (currentTime < nextSearchTime) {
            return false;
        }

        int hunger = NeedManager.get(animal, NeedType.HUNGER);

        // Urgent hunger always triggers food-seeking.
        if (hunger > HUNGRY_THRESHOLD) {

            // Only consider routine grazing periodically.
            if (currentTime < nextRoutineCheckTime) {
                return false;
            }

            nextRoutineCheckTime = currentTime + ROUTINE_CHECK_INTERVAL;

            // Chance increases as hunger decreases.
            double grazingChance = 0.10 + (100 - hunger) * 0.01;

            if (animal.getRandom().nextDouble() >= grazingChance) {
                return false;
            }
        }

        FoodProfile profile = FoodProfiles.get(animal.getType())
                .orElse(null);

        if (profile == null || !profile.accepts(FoodSourceType.VEGETATION)) {
            return false;
        }

        target = VegetationFinder.findNearestAvailable(
                animal,
                animal.blockPosition(),
                SEARCH_RADIUS
        ).orElse(null);

        if (target == null) {
            nextSearchTime = currentTime + SEARCH_RETRY_DELAY;
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        arrived = false;
        grazed = false;
        eating = false;
        eatingTicks = 0;

        BlockPos approach = target.approachPosition();

        pathFound = animal.getNavigation().moveTo(
                approach.getX() + 0.5,
                approach.getY(),
                approach.getZ() + 0.5,
                1.0
        );

        if (!pathFound) {
            nextSearchTime = animal.level().getGameTime()
                    + SEARCH_RETRY_DELAY;
        }
    }

    @Override
    public boolean canContinueToUse() {
        // Once started, finish the journey and eating animation even if
        // the animal's hunger changes in the meantime.
        return pathFound && target != null && !arrived;
    }

    @Override
    public void stop() {
        if (eating) {
            AnimalActionManager.stopEating(animal);
        }
        
        animal.getNavigation().stop();

        long currentTime = animal.level().getGameTime();

        if (grazed) {
            int cooldown = MIN_GRAZING_COOLDOWN
                    + animal.getRandom().nextInt(
                            MAX_GRAZING_COOLDOWN - MIN_GRAZING_COOLDOWN + 1
                    );

            nextSearchTime = currentTime + cooldown;
        } else if (arrived) {
            nextSearchTime = currentTime + SEARCH_RETRY_DELAY;
        }

        target = null;
        pathFound = false;
        arrived = false;
        grazed = false;
        eating = false;
        eatingTicks = 0;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }

        // Handle the eating animation once the animal has arrived.
        if (eating) {
            eatingTicks--;

            if (eatingTicks == EATING_CONSUMPTION_TICK) {
                grazed = GrazingManager.graze(animal, target);

                if (grazed) {
                    afterEating.accept(animal);
                }
            }

            if (eatingTicks <= 0) {
                arrived = true;
            }

            return;
        }

        // The food might have disappeared or changed since we found it.
        if (animal.level().getBlockState(target.foodPosition()).getBlock()
                != target.food().block()) {
            target = null;
            nextSearchTime = animal.level().getGameTime()
                    + SEARCH_RETRY_DELAY;
            return;
        }

        BlockPos approach = target.approachPosition();

        double distanceSqr = animal.distanceToSqr(
                approach.getX() + 0.5,
                approach.getY(),
                approach.getZ() + 0.5
        );

        if (distanceSqr <= ARRIVAL_DISTANCE_SQR) {
            animal.getNavigation().stop();

            eating = true;
            eatingTicks = EATING_ANIMATION_DURATION;

            AnimalActionManager.startEating(animal);
            onEatingStarted.accept(animal);
            return;
        }

        // Navigation finished without reaching the target.
        if (animal.getNavigation().isDone()) {
            pathFound = false;
            nextSearchTime = animal.level().getGameTime()
                    + SEARCH_RETRY_DELAY;
        }
    }
}
