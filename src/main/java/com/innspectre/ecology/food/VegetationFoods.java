
package com.innspectre.ecology.food;

import java.util.List;
import java.util.Optional;

import com.innspectre.ecology.block.ModBlocks;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class VegetationFoods {

    private static final List<VegetationFood> FOODS = List.of(
        new VegetationFood(
            Blocks.GRASS_BLOCK,
            new FoodNutrition(4),
            Blocks.DIRT.defaultBlockState()
        ),
        new VegetationFood(
            Blocks.TALL_GRASS,
            new FoodNutrition(8),
            Blocks.SHORT_GRASS.defaultBlockState()
        ),
        new VegetationFood(
            Blocks.SHORT_GRASS,
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
        
        // All grazing animals can eat Patched Grass, but it has very little nutritional value.
        if (block == ModBlocks.PATCHED_GRASS) {
            return Optional.of(new VegetationFood(
                ModBlocks.PATCHED_GRASS,
                new FoodNutrition(1),
                Blocks.DIRT.defaultBlockState()
            ));
        }
        
        // Sheep have their own grazing progression.
        if (animal instanceof Sheep) {
            if (block == Blocks.GRASS_BLOCK) {
                return Optional.of(new VegetationFood(
                    Blocks.GRASS_BLOCK,
                    new FoodNutrition(4),
                    ModBlocks.PATCHED_GRASS.defaultBlockState()
                ));
            }

            if (block == Blocks.TALL_GRASS) {
                return Optional.of(new VegetationFood(
                    Blocks.TALL_GRASS,
                    new FoodNutrition(8),
                    Blocks.SHORT_GRASS.defaultBlockState()
                ));
            }

            if (block == Blocks.SHORT_GRASS) {
                return Optional.of(new VegetationFood(
                    Blocks.SHORT_GRASS,
                    new FoodNutrition(8),
                    Blocks.AIR.defaultBlockState()
                ));
            }

            if (block == ModBlocks.PATCHED_GRASS) {
                return Optional.of(new VegetationFood(
                    ModBlocks.PATCHED_GRASS,
                    new FoodNutrition(4),
                    Blocks.DIRT.defaultBlockState()
                ));
            }

            return Optional.empty();
        }

        // All other animals retain the existing vegetation behaviour.
        return FOODS.stream()
            .filter(food -> food.block() == block)
            .findFirst();
    }

    public static List<VegetationFood> getAll() {
        return FOODS;
    }
}
