
package com.innspectre.ecology.food;

import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;

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

        if (!currentState.is(target.food().block())) {
            return false;
        }


        // Transform the vegetation only if eating it changes the block.
        if (target.food().changesBlock()) {

            boolean changed = animal.level().setBlock(
                    foodPos,
                    target.food().eatenResult(),
                    3
            );

            if (!changed) {
                return false;
            }
        }

        // Restore hunger even when the animal pecks without changing the block.
        NeedManager.change(
                animal,
                NeedType.HUNGER,
                target.food().nutrition().hungerRestored()
        );

        return true;

    }
}
