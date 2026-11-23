
package com.innspectre.ecology.predation;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import net.minecraft.world.entity.LivingEntity;

public final class PredationManager {

    private static final Set<UUID> PREDATION_KILLS = new HashSet<>();

    private PredationManager() {
    }

    /**
     * Marks an entity as being killed by the ecology predation system.
     */
    public static void markPredationKill(LivingEntity prey) {
        PREDATION_KILLS.add(prey.getUUID());
    }

    /**
     * Returns true if this entity is being killed by ecology predation.
     */
    public static boolean isPredationKill(LivingEntity prey) {
        return PREDATION_KILLS.contains(prey.getUUID());
    }

    /**
     * Removes the marker after death loot has been processed.
     */
    public static void clearPredationKill(LivingEntity prey) {
        PREDATION_KILLS.remove(prey.getUUID());
    }
}
