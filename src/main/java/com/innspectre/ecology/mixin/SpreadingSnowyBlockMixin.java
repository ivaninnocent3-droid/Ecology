
package com.innspectre.ecology.mixin;

import com.innspectre.ecology.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpreadingSnowyBlock.class)
public abstract class SpreadingSnowyBlockMixin {

    private static final int SPREAD_CHANCE = 16;

    @Inject(
            method = "randomTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ecology$spreadPatchedGrass(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        // Leave mycelium and other spreading blocks completely unchanged.
        if ((Object) this != Blocks.GRASS_BLOCK) {
            return;
        }

        // Grass dies back into dirt when there isn't enough light.
        if (level.getMaxLocalRawBrightness(pos.above()) < 9) {
            level.setBlock(
                    pos,
                    Blocks.DIRT.defaultBlockState(),
                    3
            );

            ci.cancel();
            return;
        }

        if (random.nextInt(SPREAD_CHANCE) != 0) {
            ci.cancel();
            return;
        }

        // Attempt to spread into nearby dirt, just like vanilla grass.
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(
                    random.nextInt(3) - 1,
                    random.nextInt(5) - 3,
                    random.nextInt(3) - 1
            );

            BlockState targetState = level.getBlockState(target);

            if (targetState.is(Blocks.DIRT)
                    && level.getFluidState(target).isEmpty()
                    && level.getMaxLocalRawBrightness(target.above()) >= 9) {

                level.setBlock(
                        target,
                        ModBlocks.PATCHED_GRASS.defaultBlockState(),
                        3
                );
            }
        }

        // We've handled this tick ourselves.
        ci.cancel();
    }
}
