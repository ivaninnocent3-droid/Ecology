
package com.innspectre.ecology.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gradually recovers into normal grass.
 *
 * Unlike vanilla grass, this block does not spread.
 */
public class PatchedGrassBlock extends Block {

    // Chance of recovery whenever this block receives a random tick.
    private static final int RECOVERY_CHANCE = 48;

    public PatchedGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        // Grass needs sufficient light to recover.
        if (level.getMaxLocalRawBrightness(pos.above()) < 9) {
            return;
        }

        // Recover gradually rather than immediately.
        if (random.nextInt(RECOVERY_CHANCE) == 0) {
            level.setBlock(
                    pos,
                    Blocks.GRASS_BLOCK.defaultBlockState(),
                    3
            );
        }
    }
}
