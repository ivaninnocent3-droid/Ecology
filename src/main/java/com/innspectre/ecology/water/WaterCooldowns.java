package com.innspectre.ecology.water;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class WaterCooldowns {

    private static final long COOLDOWN_TICKS = 600L;

    private final Map<BlockPos, Long> cooldowns = new HashMap<>();

    public void markUnreachable(BlockPos waterSource, long currentTime) {
        cooldowns.put(waterSource.immutable(), currentTime + COOLDOWN_TICKS);
    }

    public boolean isOnCooldown(BlockPos waterSource, long currentTime) {
        Long expiry = cooldowns.get(waterSource);

        if (expiry == null) {
            return false;
        }

        if (currentTime >= expiry) {
            cooldowns.remove(waterSource);
            return false;
        }

        return true;
    }
}