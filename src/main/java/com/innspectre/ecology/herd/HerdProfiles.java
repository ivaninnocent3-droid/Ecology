package com.innspectre.ecology.herd;

import java.util.Map;
import java.util.Optional;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

/**
 * The species that herd. A species with no entry here is ignored by the herd system.
 * All numbers are placeholders to tune in play.
 */
public final class HerdProfiles {

    private static final Map<EntityType<?>, HerdProfile> PROFILES = Map.of(
            EntityTypes.SHEEP, new HerdProfile(
                    24.0, 0.50,   // join range, max join chance
                    12.0, 0.50,   // form range, form chance
                    8.0, 0.25,    // comfort radius, extra comfort per member
                    40.0, 0.01,   // leave range, max leave chance
                    600           // rejoin cooldown (ticks)
            )
    );

    private HerdProfiles() {
    }

    public static Optional<HerdProfile> get(EntityType<?> type) {
        return Optional.ofNullable(PROFILES.get(type));
    }
}