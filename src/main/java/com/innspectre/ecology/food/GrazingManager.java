package com.innspectre.ecology.food;

import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.state.BlockState;

public final class GrazingManager {

    private GrazingManager() {
    }

    public static boolean graze(
            PathfinderMob animal,
            VegetationTarget target
    ) {
        if (animal.level().isClientSide()) {
            return false;
        }

        BlockPos foodPos = target.foodPosition();

        BlockState currentState = animal.level().getBlockState(foodPos);

        // Check that the food hasn't changed since we found it.
        if (!currentState.is(target.food().block())) {
            return false;
        }

        // Transform the vegetation into its eaten state.
        boolean changed = animal.level().setBlock(
                foodPos,
                target.food().eatenResult(),
                3
        );

        if (!changed) {
            return false;
        }

        // Restore hunger according to the food's nutritional value.
        NeedManager.change(
                animal,
                NeedType.HUNGER,
                target.food().nutrition().hungerRestored()
        );

        return true;
    }
}