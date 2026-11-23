package com.innspectre.ecology.animation;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.sheep.Sheep;

public final class AnimalActionManager {

    private AnimalActionManager() {
    }

    public static void startEating(PathfinderMob animal) {
        if (animal.level().isClientSide() || animal instanceof Sheep) {
            return;
        }

        animal.level().broadcastEntityEvent(
                animal,
                AnimalActionEvents.START_EATING
        );
    }

    public static void stopEating(PathfinderMob animal) {
        if (animal.level().isClientSide() || animal instanceof Sheep) {
            return;
        }

        animal.level().broadcastEntityEvent(
                animal,
                AnimalActionEvents.STOP_EATING
        );
    }

    public static void startDrinking(PathfinderMob animal) {
        if (animal.level().isClientSide()) {
            return;
        }

        animal.level().broadcastEntityEvent(
                animal,
                AnimalActionEvents.START_DRINKING
        );
    }

    public static void stopDrinking(PathfinderMob animal) {
        if (animal.level().isClientSide()) {
            return;
        }

        animal.level().broadcastEntityEvent(
                animal,
                AnimalActionEvents.STOP_DRINKING
        );
    }
}