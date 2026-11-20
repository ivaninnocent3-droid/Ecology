package com.innspectre.ecology.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Set;

public record WaterBody(List<BlockPos> blocks) {

    public Set<BlockPos> getShoreline() {
        Set<BlockPos> shoreline = new java.util.HashSet<>();

        Set<BlockPos> waterPositions = Set.copyOf(blocks);

        for (BlockPos waterPos : blocks) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbour = waterPos.relative(direction);

                if (!waterPositions.contains(neighbour)) {
                    shoreline.add(waterPos);
                    break;
                }
            }
        }

        return shoreline;
    }
}