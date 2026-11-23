package com.innspectre.ecology.food;

import net.minecraft.core.BlockPos;

public record VegetationTarget(
    BlockPos foodPosition,
    BlockPos approachPosition,
    VegetationFood food
) {
}