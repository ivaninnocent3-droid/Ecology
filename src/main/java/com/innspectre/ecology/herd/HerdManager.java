package com.innspectre.ecology.herd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

import net.minecraft.world.entity.player.Player;

import com.innspectre.ecology.recognition.RecognitionManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * The one place the rest of the mod talks to when it wants anything to do with herds.
 * The registry holds the data; this class holds the rules (same species, alive, loaded...).
 * Server side only: everything quietly does nothing on the client.
 */
public final class HerdManager {

    /** How often the cached centre is recalculated. */
    public static final long CENTRE_MAX_AGE_TICKS = 20L;

    private HerdManager() {
    }

    public static Optional<Herd> getHerd(Entity animal) {
        if (!(animal.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return HerdRegistry.get(level).getHerdOf(animal.getUUID());
    }

    /**
     * Forms a herd from the founder and any herdless animals of the same species within
     * the radius. If the founder already has a herd, returns it. Empty if there aren't
     * enough free animals.
     */
    public static Optional<Herd> formHerdAround(Mob founder, double radius) {
        if (!(founder.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        HerdRegistry registry = HerdRegistry.get(level);

        if (registry.isInHerd(founder.getUUID())) {
            return registry.getHerdOf(founder.getUUID());
        }

        EntityType<?> species = founder.getType();
        List<UUID> candidates = level.getEntitiesOfClass(
                        Mob.class,
                        founder.getBoundingBox().inflate(radius),
                        mob -> mob.isAlive()
                                && mob.getType() == species
                                && !registry.isInHerd(mob.getUUID()))
                .stream()
                // Closest first, so the founder (distance 0) is always kept if the herd fills up.
                .sorted(Comparator.comparingDouble(mob -> mob.distanceToSqr(founder)))
                .map(Mob::getUUID)
                .toList();

        return registry.createHerd(species, candidates);
    }

    /** Adds an animal to a herd if it is the right species, alive, and not already in a herd. */
    public static boolean join(Herd herd, Mob animal) {
        if (!(animal.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!animal.isAlive() || animal.getType() != herd.species()) {
            return false;
        }
        return HerdRegistry.get(level).addMember(herd.id(), animal.getUUID());
    }

    /** Takes an animal out of its herd, if it has one. Safe to call when it doesn't. */
    public static void leave(Entity animal) {
        if (animal.level() instanceof ServerLevel level) {
            HerdRegistry.get(level).removeMember(animal.getUUID());
        }
    }

    /**
     * The centre of the herd's loaded members, recalculated at most every
     * CENTRE_MAX_AGE_TICKS (the herd's radius is refreshed at the same time).
     * Empty if none of its members are loaded.
     */
    public static Optional<Vec3> centre(ServerLevel level, Herd herd) {
        long now = level.getGameTime();

        if (herd.centreIsStale(now, CENTRE_MAX_AGE_TICKS)) {
            List<Vec3> positions = new ArrayList<>(herd.size());
            double x = 0;
            double y = 0;
            double z = 0;

            for (UUID memberId : herd.members()) {
                Entity member = level.getEntity(memberId);
                if (member != null && member.isAlive()) {
                    positions.add(member.position());
                    x += member.getX();
                    y += member.getY();
                    z += member.getZ();
                }
            }

            if (!positions.isEmpty()) {
                int loaded = positions.size();
                Vec3 centre = new Vec3(x / loaded, y / loaded, z / loaded);

                // The radius comes from the same members, so it costs one extra cheap loop.
                double radius = 0;
                for (Vec3 position : positions) {
                    double dx = position.x - centre.x;
                    double dz = position.z - centre.z;
                    radius = Math.max(radius, Math.sqrt(dx * dx + dz * dz));
                }

                herd.setCentre(centre, radius, now);
            }
        }

        return herd.centre();
    }

    /** How many of the herd's members are currently loaded. */
    public static int loadedMembers(ServerLevel level, Herd herd) {
        int loaded = 0;
        for (UUID memberId : herd.members()) {
            Entity member = level.getEntity(memberId);
            if (member != null && member.isAlive()) {
                loaded++;
            }
        }
        return loaded;
    }

    /**
     * Records a player's attack on an animal for the rest of its herd.
     * Loaded members react immediately; unloaded members retain a pending reaction.
     */
    public static void recordHerdIncident(Entity victim, Player player) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }

        HerdRegistry registry = HerdRegistry.get(level);
        Optional<Herd> optionalHerd = registry.getHerdOf(victim.getUUID());

        if (optionalHerd.isEmpty()) {
            return;
        }

        Herd herd = optionalHerd.get();
        UUID playerId = player.getUUID();

        // Exclude the animal that was killed.
        List<UUID> recipients = herd.members().stream()
                .filter(memberId -> !memberId.equals(victim.getUUID()))
                .toList();

        // Queue everyone first, including unloaded members.
        registry.addPendingReaction(herd.id(), playerId, recipients);

        // Apply immediately to members that are currently loaded.
        for (UUID memberId : recipients) {
            Entity member = level.getEntity(memberId);

            if (member != null && member.isAlive()) {
                applyHerdIncident(member, playerId);
                registry.removePendingReaction(herd.id(), playerId, memberId);
            }
        }
    }

    /** Applies the actual recognition and relationship changes. */
    public static void applyHerdIncident(Entity animal, UUID playerId) {
        RecognitionManager.changeRelationship(animal, playerId, -80);
        RecognitionManager.changeRecognition(animal, playerId, 30);
    }
}