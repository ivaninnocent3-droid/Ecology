
package com.innspectre.ecology.need;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;

public final class SurvivalEvents {

    private static final long DAMAGE_INTERVAL = 20L;
    private static final float DAMAGE_AMOUNT = 1.0F;

    private SurvivalEvents() {
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(SurvivalEvents::tick);
    }

    private static void tick(ServerLevel level) {
        if (level.getGameTime() % DAMAGE_INTERVAL != 0) {
            return;
        }

        for (var entity : level.getAllEntities()) {
            if (!(entity instanceof Sheep sheep) || !sheep.isAlive()) {
                continue;
            }

            boolean starving =
                    NeedManager.get(sheep, NeedType.HUNGER) <= 0;

            boolean dehydrated =
                    NeedManager.get(sheep, NeedType.THIRST) <= 0;

            if (starving || dehydrated) {
                sheep.hurtServer(
                        level,
                        level.damageSources().starve(),
                        DAMAGE_AMOUNT
                );
            }
        }
    }
}
