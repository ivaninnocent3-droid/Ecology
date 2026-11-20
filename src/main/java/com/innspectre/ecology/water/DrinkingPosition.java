package com.innspectre.ecology.water;

import net.minecraft.core.BlockPos;

public record DrinkingPosition(
        BlockPos position,
        BlockPos waterSource,
        Type type
) {

    public enum Type {
        BANK,
        SHALLOW_WATER
    }
}