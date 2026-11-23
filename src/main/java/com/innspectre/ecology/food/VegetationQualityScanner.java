
package com.innspectre.ecology.food;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class VegetationQualityScanner {

    private static final int SEARCH_RADIUS = 16;
    private static final int VERTICAL_SEARCH_RANGE = 4;

    private static final double BASE_MINIMUM_SCORE = 18.0;
    private static final double ADDITIONAL_ANIMAL_SCORE = 8.0;

    private VegetationQualityScanner() {
    }

    public static double calculateScore(
            ServerLevel level,
            PathfinderMob animal,
            BlockPos centre
    ) {
        FoodProfile profile = FoodProfiles.get(animal.getType())
                .orElse(null);

        if (profile == null
                || !profile.accepts(FoodSourceType.VEGETATION)) {
            return 0.0;
        }

        double score = 0.0;
        int radiusSquared = SEARCH_RADIUS * SEARCH_RADIUS;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {

                double distanceSquared = x * x + z * z;

                if (distanceSquared > radiusSquared) {
                    continue;
                }

                double distanceWeight =
                        1.0 / (1.0 + distanceSquared / 64.0);

                for (int y = -VERTICAL_SEARCH_RANGE;
                        y <= VERTICAL_SEARCH_RANGE; y++) {

                    BlockPos foodPos = centre.offset(x, y, z);

                    // Never load chunks just to assess vegetation.
                    if (level.getChunk(
                            foodPos.getX() >> 4,
                            foodPos.getZ() >> 4,
                            ChunkStatus.FULL,
                            false
                    ) == null) {
                        continue;
                    }

                    var state = level.getBlockState(foodPos);

                    var food = VegetationFoods.get(
                            state.getBlock(),
                            animal
                    ).orElse(null);

                    if (food == null) {
                        continue;
                    }

                    // Avoid counting the same chicken food twice.
                    if (animal instanceof net.minecraft.world.entity.animal.chicken.Chicken
                            && food.eatenResult() == null
                            && (level.getBlockState(foodPos.above()).is(Blocks.TALL_GRASS)
                            || level.getBlockState(foodPos.above()).is(Blocks.SHORT_GRASS))) {
                        continue;
                    }

                    int nutrition =
                            food.nutrition().hungerRestored();

                    // Nutrition 1 is our depleted-ground baseline.
                    double usefulNutrition = Math.max(0, nutrition - 1);

                    score += usefulNutrition * distanceWeight;
                }
            }
        }

        return score;
    }

    public static double minimumScoreForPopulation(int animalCount) {
        int population = Math.max(1, animalCount);

        return BASE_MINIMUM_SCORE
                + (population - 1) * ADDITIONAL_ANIMAL_SCORE;
    }

    public static boolean isAreaSustainable(
            double score,
            int animalCount
    ) {
        return score > minimumScoreForPopulation(animalCount);
    }
}
