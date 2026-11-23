package com.innspectre.ecology.block;

import com.innspectre.ecology.Ecology;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.core.Registry;

public final class ModBlocks {

    private ModBlocks() {
    }

    public static final ResourceKey<Block> PATCHED_GRASS_KEY =
            ResourceKey.create(Registries.BLOCK, Ecology.id("patched_grass"));

    public static final ResourceKey<Item> PATCHED_GRASS_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, Ecology.id("patched_grass"));

    public static final Block PATCHED_GRASS = registerBlock(
            PATCHED_GRASS_KEY,
            BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)
                    .setId(PATCHED_GRASS_KEY)
    );

    private static Block registerBlock(
            ResourceKey<Block> key,
            BlockBehaviour.Properties properties
    ) {
        Block block = Registry.register(
                BuiltInRegistries.BLOCK,
                key,
                new PatchedGrassBlock(properties)
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                PATCHED_GRASS_ITEM_KEY,
                new BlockItem(
                        block,
                        new Item.Properties()
                                .setId(PATCHED_GRASS_ITEM_KEY)
                                .useBlockDescriptionPrefix()
                )
        );

        return block;
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(creativeTab -> creativeTab.accept(PATCHED_GRASS.asItem()));
    }
}