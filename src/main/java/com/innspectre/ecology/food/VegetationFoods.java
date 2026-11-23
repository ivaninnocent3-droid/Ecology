
package com.innspectre.ecology.food;

import java.util.List;
import java.util.Optional;

import com.innspectre.ecology.block.*;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;


public final class VegetationFoods {

    private static final List<VegetationFood> FOODS = List.of(
        new VegetationFood(
            Blocks.GRASS_BLOCK,
            new FoodNutrition(4),
            ModBlocks.PATCHED_GRASS.defaultBlockState()
        ),
        new VegetationFood(
            Blocks.TALL_GRASS,
            new FoodNutrition(10),
            Blocks.SHORT_GRASS.defaultBlockState()
        ),
        new VegetationFood(
            Blocks.SHORT_GRASS,
            new FoodNutrition(6),
            Blocks.AIR.defaultBlockState()
        ),
        new VegetationFood(
            Blocks.BUSH,
            new FoodNutrition(8),
            Blocks.AIR.defaultBlockState()
        )
    );

    private VegetationFoods() {
    }

    public static Optional<VegetationFood> get(
            Block block,
            PathfinderMob animal
    ) {
        // Chickens can peck the ground without changing it.
        if (animal instanceof Chicken && isPeckableGround(block)) {
            return Optional.of(new VegetationFood(
                block,
                new FoodNutrition(1),
                null
            ));
        }

        // Patched Grass provides very little nutrition.
        if (block == ModBlocks.PATCHED_GRASS) {
            return Optional.of(new VegetationFood(
                ModBlocks.PATCHED_GRASS,
                new FoodNutrition(1),
                Blocks.DIRT.defaultBlockState()
            ));
        }

        // All animals use the same grass and tall/short grass progression.
        return FOODS.stream()
            .filter(food -> food.block() == block)
            .findFirst();
    }

    private static boolean isPeckableGround(Block block) {
        return block == Blocks.GRASS_BLOCK
            || block == Blocks.DIRT
            || block == Blocks.COARSE_DIRT
            || block == Blocks.ROOTED_DIRT
            || block == Blocks.PODZOL
            || block == Blocks.MYCELIUM
            || block == Blocks.MOSS_BLOCK
            || block == Blocks.MUD
            || block == ModBlocks.PATCHED_GRASS;
    }

    public static List<VegetationFood> getAll() {
        return FOODS;
    }
}
