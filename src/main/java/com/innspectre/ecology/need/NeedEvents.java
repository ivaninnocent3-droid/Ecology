
package com.innspectre.ecology.need;

import com.innspectre.ecology.food.FoodProfiles;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.animal.Animal;

/**
 * Starts tracking needs when a supported animal loads.
 */
public final class NeedEvents {

    private NeedEvents() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!(entity instanceof Animal animal)) {
                return;
            }

            // Only animals with registered Ecology food profiles
            // participate in the hunger and thirst systems.
            if (FoodProfiles.get(animal.getType()).isEmpty()) {
                return;
            }

            NeedManager.ensureStarted(animal, NeedType.HUNGER);
            NeedManager.ensureStarted(animal, NeedType.THIRST);
        });
    }
}
