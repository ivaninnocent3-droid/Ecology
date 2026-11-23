package com.innspectre.ecology.client;

import java.util.List;

import com.innspectre.ecology.block.ModBlocks;

import net.fabricmc.api.ClientModInitializer;
// ...plus the rest of the imports, which your IDE can add (see below)
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.state.BlockState;


public class EcologyClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockColorRegistry.register(List.of(new BlockTintSource() {
            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return ARGB.opaque(BiomeColors.getAverageGrassColor(level, pos));
            }

            @Override
            public int color(BlockState state) {
                return ARGB.opaque(GrassColor.getDefaultColor());
            }
        }), ModBlocks.PATCHED_GRASS);
    }
}