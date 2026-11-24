
package com.innspectre.ecology.need;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.animal.Animal;

/**
 * Starts tracking needs when an animal loads.
 */
public final class NeedEvents {

    private NeedEvents() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Animal) {
                NeedManager.ensureStarted(entity, NeedType.HUNGER);
                NeedManager.ensureStarted(entity, NeedType.THIRST);
            }
        });
    }
}
