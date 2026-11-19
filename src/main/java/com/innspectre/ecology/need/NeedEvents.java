package com.innspectre.ecology.need;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.animal.sheep.Sheep;

/** Replaces HungerTicker: no per-tick loop, just start tracking when an entity loads. */
public class NeedEvents {

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            // Still sheep-only for now; widen this once the diet system is in.
            if (entity instanceof Sheep) {
                NeedManager.ensureStarted(entity, NeedType.HUNGER);
            }
        });
    }
}