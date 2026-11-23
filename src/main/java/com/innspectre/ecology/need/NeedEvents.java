package com.innspectre.ecology.need;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;

/**
 * Starts tracking needs when an entity loads.
 */
public class NeedEvents {

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Sheep || entity instanceof Wolf) {
                NeedManager.ensureStarted(entity, NeedType.HUNGER);
                NeedManager.ensureStarted(entity, NeedType.THIRST);
            }
        });
    }
}