package com.innspectre.ecology.food;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class VegetationFinder {

    private static final int VERTICAL_SEARCH_RANGE = 4;
    private static final int MAX_PATHFINDING_ATTEMPTS = 8;

    private VegetationFinder() {
    }

    public static Optional<VegetationTarget> findNearestAvailable(
            PathfinderMob animal,
            BlockPos centre,
            int radius
    ) {

        // Only herbivores and omnivores should search for vegetation.
        FoodProfile profile = FoodProfiles.get(animal.getType())
                .orElse(null);

        if (profile == null
                || !profile.accepts(FoodSourceType.VEGETATION)) {
            return Optional.empty();
        }

        List<VegetationTarget> candidates = new ArrayList<>();

        // Search the surrounding area for edible vegetation.
        for (int x = -radius; x <= radius; x++) {
            for (int y = -VERTICAL_SEARCH_RANGE;
                    y <= VERTICAL_SEARCH_RANGE; y++) {

                for (int z = -radius; z <= radius; z++) {

                    BlockPos foodPos = centre.offset(x, y, z);

                    if (animal.level().getChunk(
                            foodPos.getX() >> 4,
                            foodPos.getZ() >> 4,
                            ChunkStatus.FULL,
                            false
                    ) == null) {
                        continue;
                    }

                    BlockState state = animal.level().getBlockState(foodPos);

                    VegetationFood food = VegetationFoods
                            .get(state.getBlock(), animal)
                            .orElse(null);

                    if (food == null) {
                        continue;
                    }
                    if (animal instanceof Chicken
                            && food.eatenResult() == null
                            && (animal.level().getBlockState(foodPos.above()).is(net.minecraft.world.level.block.Blocks.TALL_GRASS)
                                || animal.level().getBlockState(foodPos.above()).is(net.minecraft.world.level.block.Blocks.SHORT_GRASS))) {
                        continue;
                    }

                    // Solid food blocks require the animal to stand above them.
                    BlockPos approachPos = state.isCollisionShapeFullBlock(
                            animal.level(),
                            foodPos
                    ) ? foodPos.above() : foodPos;

                    candidates.add(new VegetationTarget(
                            foodPos,
                            approachPos,
                            food
                    ));
                }
            }
        }

        // Prefer nutritious food, while still favouring nearby sources.
        candidates.sort(
                Comparator.comparingDouble(target -> {
                    double distance = centre.distSqr(target.approachPosition());
                    int nutrition = target.food().nutrition().hungerRestored();

                    return distance - nutrition * nutrition * 4.0;
                })
        );


        int pathfindingAttempts = 0;

        for (VegetationTarget candidate : candidates) {

            if (pathfindingAttempts >= MAX_PATHFINDING_ATTEMPTS) {
                break;
            }

            pathfindingAttempts++;

            var path = animal.getNavigation().createPath(
                    candidate.approachPosition(),
                    0
            );

            if (path != null && path.canReach()) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }
}