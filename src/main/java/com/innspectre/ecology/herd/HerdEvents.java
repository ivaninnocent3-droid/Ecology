package com.innspectre.ecology.herd;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Keeps herd membership correct as animals die or disappear.
 *
 * Animals that are merely unloaded (chunk unloaded, player left the area) keep their
 * membership, as designed. Animals that are removed for good, or that leave the dimension,
 * are taken out of their herd.
 */
public final class HerdEvents {

    private HerdEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof Mob) {
                HerdManager.leave(entity);
            }
        });

        // Also covers despawning and /kill, which don't always go through the death event.
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (!(entity instanceof Mob)) {
                return;
            }
            Entity.RemovalReason reason = entity.getRemovalReason();
            if (reason != null
                    && (reason.shouldDestroy() || reason == Entity.RemovalReason.CHANGED_DIMENSION)) {
                HerdManager.leave(entity);
            }
        });
    }
}