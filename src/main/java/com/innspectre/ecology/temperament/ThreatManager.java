package com.innspectre.ecology.temperament;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.PathfinderMob;

/**
 * Holds short-lived threats perceived by loaded animals.
 *
 * Threats are deliberately runtime-only. They represent an animal currently
 * responding to danger rather than a persistent memory.
 */
public final class ThreatManager {

    private static final long THREAT_MEMORY_TICKS = 60L;

    private static final Map<UUID, ThreatMemory> THREATS = new HashMap<>();

    private record ThreatMemory(
            Threat threat,
            long expiresAt
    ) {}

    private ThreatManager() {
    }

    /**
     * Gives an animal a threat to react to.
     *
     * A stronger threat replaces a weaker one. If the existing threat is stronger,
     * it is retained but its memory is refreshed.
     */
    public static void threaten(
            PathfinderMob animal,
            Threat threat
    ) {
        threaten(animal, threat, THREAT_MEMORY_TICKS);
    }

    public static void threaten(
            PathfinderMob animal,
            Threat threat,
            long memoryTicks
    ) {
        long now = animal.level().getGameTime();
        long expiryTime = now + Math.max(1L, memoryTicks);

        ThreatMemory existing = THREATS.get(animal.getUUID());

        if (existing == null
                || threat.strength() >= existing.threat().strength()) {

            THREATS.put(
                    animal.getUUID(),
                    new ThreatMemory(
                            threat,
                            expiryTime
                    )
            );

            return;
        }

        // Existing threat is stronger, but the animal is still actively threatened.
        THREATS.put(
                animal.getUUID(),
                new ThreatMemory(
                        existing.threat(),
                        expiryTime
                )
        );
    }
    public static Threat get(PathfinderMob animal) {
        ThreatMemory memory = THREATS.get(animal.getUUID());

        if (memory == null) {
            return null;
        }

        if (animal.level().getGameTime() >= memory.expiresAt()) {
            THREATS.remove(animal.getUUID());
            return null;
        }

        return memory.threat();
    }

    public static boolean isThreatened(PathfinderMob animal) {
        return get(animal) != null;
    }

    public static void clear(PathfinderMob animal) {
        THREATS.remove(animal.getUUID());
    }

    /**
     * Removes stale threats periodically.
     */
    public static void cleanup(long currentTime) {
        THREATS.entrySet().removeIf(
                entry -> currentTime >= entry.getValue().expiresAt()
        );
    }
}