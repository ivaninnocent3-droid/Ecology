package com.innspectre.ecology.herd;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Decides when animals join, form and leave herds.
 *
 * Each herdable animal is checked once every CHECK_INTERVAL_TICKS ticks. The checks are
 * spread out by entity id so they don't all happen on the same tick. Only a small list of
 * loaded herdable animals is walked each tick, never every entity in the level.
 *
 * Leaving is judged by distance to the herd's cached centre. Joining is judged by distance
 * to the herd's edge (centre distance minus its radius), so big herds are easier to find.
 */
public final class HerdTicker {

    public static final int CHECK_INTERVAL_TICKS = 40;

    /** An animal that just left a herd can't rejoin that same herd until this tick. */
    private record Cooldown(UUID herdId, long untilTick) {
    }

    private static final Map<ServerLevel, Set<Mob>> TRACKED = new WeakHashMap<>();
    private static final Map<UUID, Cooldown> COOLDOWNS = new HashMap<>();

    private HerdTicker() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Mob mob && HerdProfiles.get(mob.getType()).isPresent()) {
                TRACKED.computeIfAbsent(level, key -> new LinkedHashSet<>()).add(mob);
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Mob mob) {
                Set<Mob> tracked = TRACKED.get(level);
                if (tracked != null) {
                    tracked.remove(mob);
                }
                COOLDOWNS.remove(mob.getUUID());
            }
        });

        ServerTickEvents.END_LEVEL_TICK.register(HerdTicker::onLevelTick);
    }

    private static void onLevelTick(ServerLevel level) {
        Set<Mob> tracked = TRACKED.get(level);
        if (tracked == null || tracked.isEmpty()) {
            return;
        }

        HerdRegistry registry = HerdRegistry.get(level);
        long tick = level.getGameTime();

        for (Mob mob : tracked) {
            // Each animal gets its turn once per interval, offset by its id.
            if ((mob.getId() + tick) % CHECK_INTERVAL_TICKS != 0 || !mob.isAlive()) {
                continue;
            }

            HerdProfile profile = HerdProfiles.get(mob.getType()).orElse(null);
            if (profile == null) {
                continue;
            }

            Optional<Herd> current = registry.getHerdOf(mob.getUUID());
            if (current.isPresent()) {
                maybeLeave(level, mob, current.get(), profile, tick);
            } else {
                maybeJoinOrForm(level, registry, mob, profile, tick);
            }
        }
    }

    // ---- leaving ----

        
    private static final int WATER_EXPLORATION_THRESHOLD = 50;

    /**
     * Maximum chance of leaving per check while behaving normally.
     */
    private static final double EXPLORING_LEAVE_CHANCE = 0.002;

    private static void maybeLeave(
            ServerLevel level,
            Mob mob,
            Herd herd,
            HerdProfile profile,
            long tick
    ) {
        Optional<Vec3> centre = HerdManager.centre(level, herd);

        if (centre.isEmpty()) {
            return;
        }

        double distance = distanceToHerd(mob, centre.get());
        double comfort = profile.comfortFor(herd.size());

        if (distance <= comfort) {
            return;
        }

        // A sheep exploring for water should normally retain herd membership,
        // even when it travels well beyond the usual comfort distance.
        boolean exploringForWater =
                NeedManager.get(mob, NeedType.THIRST)
                        < WATER_EXPLORATION_THRESHOLD;

        if (exploringForWater) {
            if (mob.getRandom().nextDouble() < EXPLORING_LEAVE_CHANCE) {
                leaveHerd(mob, herd, profile, tick);
            }

            return;
        }

        // Normal distance-based leaving behaviour.
        double span = Math.max(1.0, profile.leaveRange() - comfort);
        double t = Math.min(1.0, (distance - comfort) / span);
        double chance = profile.maxLeaveChance() * t * t;

        if (mob.getRandom().nextDouble() < chance) {
            leaveHerd(mob, herd, profile, tick);
        }
    }

    private static void leaveHerd(
            Mob mob,
            Herd herd,
            HerdProfile profile,
            long tick
    ) {
        UUID herdId = herd.id();

        HerdManager.leave(mob);

        COOLDOWNS.put(
                mob.getUUID(),
                new Cooldown(herdId, tick + profile.rejoinCooldownTicks())
        );
    }


    // ---- joining and forming ----

    private static void maybeJoinOrForm(ServerLevel level, HerdRegistry registry, Mob mob,
                                        HerdProfile profile, long tick) {
        Herd nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Herd herd : registry.herds()) {
            if (herd.species() != mob.getType() || herd.isFull() || onCooldown(mob, herd, tick)) {
                continue;
            }
            Optional<Vec3> centre = HerdManager.centre(level, herd);
            if (centre.isEmpty()) {
                continue;
            }
            double distance = distanceToEdge(mob, centre.get(), herd, profile);
            if (distance < nearestDistance) {
                nearest = herd;
                nearestDistance = distance;
            }
        }

        if (nearest != null && nearestDistance < profile.joinRange()) {
            // Closer means likelier; squared so far-away herds are only rarely joined.
            double closeness = 1.0 - nearestDistance / profile.joinRange();
            double chance = profile.maxJoinChance() * closeness * closeness;
            if (mob.getRandom().nextDouble() < chance) {
                HerdManager.join(nearest, mob);
            }
            return; // a herd is in range, so don't also start a new one
        }

        // No herd in range: maybe start one with herdless neighbours.
        if (mob.getRandom().nextDouble() < profile.formChance()) {
            HerdManager.formHerdAround(mob, profile.formRange());
        }
    }

    private static boolean onCooldown(Mob mob, Herd herd, long tick) {
        Cooldown cooldown = COOLDOWNS.get(mob.getUUID());
        if (cooldown == null) {
            return false;
        }
        if (tick >= cooldown.untilTick()) {
            COOLDOWNS.remove(mob.getUUID());
            return false;
        }
        return cooldown.herdId().equals(herd.id());
    }

    /**
     * Distance from an animal to the edge of a herd's core. The core is the herd's measured
     * radius, but never more than its comfort radius, so one far-off straggler can't make a
     * herd look enormous. 0 means the animal is inside the core.
     */
    private static double distanceToEdge(Mob mob, Vec3 centre, Herd herd, HerdProfile profile) {
        double core = Math.min(herd.radius(), profile.comfortFor(herd.size()));
        return Math.max(0.0, distanceToHerd(mob, centre) - core);
    }

    /** Horizontal distance from an animal to a herd's cached centre. */
    private static double distanceToHerd(Mob mob, Vec3 centre) {
        double dx = mob.getX() - centre.x;
        double dz = mob.getZ() - centre.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}