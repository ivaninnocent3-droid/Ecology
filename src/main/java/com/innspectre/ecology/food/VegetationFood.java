
package com.innspectre.ecology.food;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record VegetationFood(
    Block block,
    FoodNutrition nutrition,
    BlockState eatenResult
) {
    public boolean changesBlock() {
        return eatenResult != null;
    }
}
