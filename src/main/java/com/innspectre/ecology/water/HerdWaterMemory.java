
package com.innspectre.ecology.water;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

import com.innspectre.ecology.herd.Herd;
import com.innspectre.ecology.herd.HerdManager;

/**
 * Stores recently discovered water locations for each herd.
 *
 * Knowledge is temporary and runtime-only. Herd membership remains the
 * responsibility of HerdRegistry.
 *
 * A location is shared only with thirsty herd members within SHARE_DISTANCE.
 */
public final class HerdWaterMemory {

    private static final long MEMORY_DURATION = 1200L;
    private static final double SHARE_DISTANCE = 64.0;

    private record Memory(BlockPos waterSource, long lastConfirmed) {
    }

    // Each dimension has its own set of herd water memories.
    private static final Map<ServerLevel, Map<UUID, Memory>> MEMORIES =
            new WeakHashMap<>();

    private HerdWaterMemory() {
    }

    /**
     * Records a water source after a herd member has successfully reached it
     * and begun drinking.
     */
    public static void reportWater(Entity animal, BlockPos waterSource) {
        if (!(animal.level() instanceof ServerLevel level)) {
            return;
        }

        Optional<Herd> herd = HerdManager.getHerd(animal);

        if (herd.isEmpty()) {
            return;
        }

        if (!isValidWaterSource(level, waterSource)) {
            return;
        }

        Map<UUID, Memory> memories =
                MEMORIES.computeIfAbsent(level, key -> new HashMap<>());

        memories.put(
                herd.get().id(),
                new Memory(waterSource.immutable(), level.getGameTime())
        );
    }

    /**
     * Returns the herd's known water source if it is still valid,
     * recently confirmed, loaded, and close enough to share.
     */
    public static Optional<BlockPos> getKnownWater(Entity animal) {
        if (!(animal.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }

        Optional<Herd> herd = HerdManager.getHerd(animal);

        if (herd.isEmpty()) {
            return Optional.empty();
        }

        Map<UUID, Memory> memories = MEMORIES.get(level);

        if (memories == null) {
            return Optional.empty();
        }

        UUID herdId = herd.get().id();
        Memory memory = memories.get(herdId);

        if (memory == null) {
            return Optional.empty();
        }

        long now = level.getGameTime();

        // Forget old knowledge.
        if (now - memory.lastConfirmed() > MEMORY_DURATION) {
            memories.remove(herdId);
            return Optional.empty();
        }

        BlockPos source = memory.waterSource();

        // If the chunk is unloaded, retain the memory but don't use it yet.
        if (!level.hasChunkAt(source)) {
            return Optional.empty();
        }

        // The water may have been removed or changed.
        if (!isValidWaterSource(level, source)) {
            memories.remove(herdId);
            return Optional.empty();
        }

        // Don't make animals travel indefinitely to a distant discovery.
        if (animal.blockPosition().distSqr(source)
                > SHARE_DISTANCE * SHARE_DISTANCE) {
            return Optional.empty();
        }

        return Optional.of(source);
    }

    private static boolean isValidWaterSource(
            ServerLevel level,
            BlockPos position
    ) {
        return level.getBlockState(position).is(Blocks.WATER)
                && level.getFluidState(position).getAmount() == 8;
    }
}
