
package com.innspectre.ecology.spawn;

import java.util.List;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext.MobSpawnSettingsContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;

import net.minecraft.util.random.Weighted;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

import com.innspectre.ecology.Ecology;

public final class AnimalSpawning {

    private AnimalSpawning() {
    }

    public static void register() {

        BiomeModifications.create(Ecology.id("animal_spawning"))
                .add(
                        ModificationPhase.REPLACEMENTS,
                        context -> true,
                        context -> {
                            MobSpawnSettingsContext spawns =
                                    context.getMobSpawnSettings();

                            resizeSpawnGroup(spawns, EntityTypes.SHEEP, 6, 12);
                            resizeSpawnGroup(spawns, EntityTypes.COW, 6, 12);
                            resizeSpawnGroup(spawns, EntityTypes.PIG, 3, 6);
                            resizeSpawnGroup(spawns, EntityTypes.CHICKEN, 6, 8);
                            resizeSpawnGroup(spawns, EntityTypes.WOLF, 2, 6);
                        }
                );
    }

    private static void resizeSpawnGroup(
            MobSpawnSettingsContext spawns,
            EntityType<?> entityType,
            int minSize,
            int maxSize
    ) {
        // Find the existing spawn entries for this animal.
        List<Weighted<SpawnerData>> existing =
                spawns.getMobs(MobCategory.CREATURE);

        int totalWeight = existing.stream()
                .filter(entry -> entry.value().type() == entityType)
                .mapToInt(Weighted::weight)
                .sum();

        // Don't introduce animals into biomes where they don't naturally spawn.
        if (totalWeight == 0) {
            return;
        }

        // Replace the old entry while retaining its original spawn weight.
        spawns.removeSpawnsOfEntityType(entityType);

        spawns.addSpawn(
                entityType,
                totalWeight,
                UniformInt.of(minSize, maxSize)
        );
    }
}
