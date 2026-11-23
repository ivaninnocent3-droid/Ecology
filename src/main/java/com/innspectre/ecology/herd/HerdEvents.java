package com.innspectre.ecology.herd;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;


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
                // Record the incident while the dead animal is still in its herd.
                if (damageSource.getEntity() instanceof Player player) {
                    HerdManager.recordHerdIncident(entity, player);
                }

                // Remove the dead animal afterwards.
                HerdManager.leave(entity);
            }
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                return;
            }

            HerdRegistry registry = HerdRegistry.get(level);

            var optionalHerd = registry.getHerdOf(mob.getUUID());
            if (optionalHerd.isEmpty()) {
                return;
            }

            Herd herd = optionalHerd.get();

            // Check every player who has an incident waiting to be processed.
            for (UUID playerId : registry.getPendingReactionPlayerIds(herd.id())) {
                if (registry.getPendingReactions(herd.id(), playerId)
                        .contains(mob.getUUID())) {

                    HerdManager.applyHerdIncident(mob, playerId);

                    // Remove this animal from the queue so it cannot receive
                    // the same reaction again.
                    registry.removePendingReaction(
                            herd.id(), playerId, mob.getUUID());
                }
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