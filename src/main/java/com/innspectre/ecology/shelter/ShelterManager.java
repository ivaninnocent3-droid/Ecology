package com.innspectre.ecology.shelter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.innspectre.ecology.herd.Herd;
import com.innspectre.ecology.herd.HerdManager;
import com.innspectre.ecology.herd.HerdRegistry;
import com.innspectre.ecology.need.NeedManager;
import java.util.WeakHashMap;
import net.minecraft.world.entity.PathfinderMob;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.phys.Vec3;

public final class ShelterManager {

    private static final long SEARCH_RETRY_DELAY = 200L;
    private static final long MAX_SEARCH_TIME = 6000L;

    // Runtime-only shelter state for animals that do not belong to a herd.
    private static final Map<UUID, IndividualShelterState> INDIVIDUAL_SHELTERS =
            new LinkedHashMap<>();

    private static final class IndividualShelterState {
        private final long searchStartedAt;
        private long nextSearchTick;
        private ShelterTarget target;

        private IndividualShelterState(long currentTime) {
            this.searchStartedAt = currentTime;
            this.nextSearchTick = currentTime;
        }
    }




    private ShelterManager() {
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(
                ShelterManager::tickLevel
        );
    }

    public static boolean isShelterTime(ServerLevel level) {
        Holder<WorldClock> overworldClock = level.registryAccess()
                .lookupOrThrow(Registries.WORLD_CLOCK)
                .getOrThrow(WorldClocks.OVERWORLD);

        long time = level.getServer()
                .clockManager()
                .getInstance(overworldClock)
                .totalTicks();

        long timeOfDay = Math.floorMod(time, 24000L);

        return timeOfDay >= 12000L && timeOfDay < 23000L;
    }

    private static void tickLevel(ServerLevel level) {
        long currentTime = level.getGameTime();

        HerdRegistry registry = HerdRegistry.get(level);

        Map<UUID, Entity> loadedEntities = new LinkedHashMap<>();

        for (Entity entity : level.getAllEntities()) {
            loadedEntities.put(entity.getUUID(), entity);
        }

        if (!isShelterTime(level)) {
            INDIVIDUAL_SHELTERS.clear();
        } else {
            // Process supported animals that aren't currently members of a herd.
            for (Entity entity : loadedEntities.values()) {
                if (!(entity instanceof PathfinderMob animal)
                        || !animal.isAlive()
                        || HerdManager.getHerd(animal).isPresent()
                        || ShelterProfiles.get(animal.getType()).isEmpty()) {
                    continue;
                }

                processIndividualAnimal(level, animal, currentTime);
            }
        }

        for (Herd herd : registry.herds()) {

            if (!isShelterTime(level)) {
                herd.resetShelterState();
                continue;
            }

            herd.startShelterSearch(currentTime);

            // A target has already been selected.
            if (herd.shelterTarget().isPresent()) {
                continue;
            }

            // Search period has expired: settle where the herd currently is.
            if (currentTime - herd.shelterSearchStartedAt()
                    >= MAX_SEARCH_TIME) {

                createFallbackTarget(level, herd, loadedEntities);
                continue;
            }

            if (currentTime < herd.nextShelterSearchTick()) {
                continue;
            }

            var profile = ShelterProfiles.get(herd.species());

            if (profile.isEmpty()) {
                continue;
            }

            var centre = HerdManager.centre(level, herd);

            if (centre.isEmpty()) {
                herd.setNextShelterSearchTick(
                        currentTime + SEARCH_RETRY_DELAY
                );
                continue;
            }

            List<UUID> loadedMemberIds = herd.members().stream()
                    .filter(loadedEntities::containsKey)
                    .toList();

            if (loadedMemberIds.isEmpty()) {
                herd.setNextShelterSearchTick(
                        currentTime + SEARCH_RETRY_DELAY
                );
                continue;
            }

            var target = ShelterFinder.find(
                    level,
                    centre.get(),
                    profile.get(),
                    loadedMemberIds
            );

            if (target.isPresent()) {
                herd.setShelterTarget(target.get());
            } else {
                herd.setNextShelterSearchTick(
                        currentTime + SEARCH_RETRY_DELAY
                );
            }
        }

        // Advance needs using each animal's actual shelter status.
        for (Entity entity : loadedEntities.values()) {
            boolean sheltered = isProperlySheltered(level, entity);
            NeedManager.advance(entity, sheltered);
        }
    }

    private static void createFallbackTarget(
            ServerLevel level,
            Herd herd,
            Map<UUID, Entity> loadedEntities
    ) {
        Map<UUID, BlockPos> assignments = new LinkedHashMap<>();

        BlockPos centre = herd.centre()
                .map(BlockPos::containing)
                .orElseGet(() -> {
                    for (UUID member : herd.members()) {
                        Entity entity = loadedEntities.get(member);

                        if (entity != null) {
                            return entity.blockPosition();
                        }
                    }

                    return BlockPos.ZERO;
                });

        for (UUID member : herd.members()) {
            Entity entity = loadedEntities.get(member);

            if (entity != null) {
                // Each loaded animal stays where it currently is.
                assignments.put(member, entity.blockPosition());
            }
        }

        if (assignments.isEmpty()) {
            return;
        }

        herd.setShelterTarget(new ShelterTarget(
                centre,
                assignments,
                false
        ));
    }

    private static boolean isProperlySheltered(
            ServerLevel level,
            Entity entity
    ) {
        if (!(entity instanceof PathfinderMob animal)) {
            return false;
        }

        ShelterTarget target = getShelterTarget(animal).orElse(null);

        if (target == null || !target.roofed()) {
            return false;
        }

        BlockPos assignedPosition = target
                .positionFor(animal.getUUID())
                .orElse(null);

        if (assignedPosition == null) {
            return false;
        }

        // The animal must actually have reached its resting position.
        if (animal.distanceToSqr(
                assignedPosition.getX() + 0.5,
                assignedPosition.getY(),
                assignedPosition.getZ() + 0.5
        ) > 2.25) {
            return false;
        }

        // The roof and resting position must still be valid.
        return ShelterFinder.isValidSpot(level, assignedPosition);
    }

    private static void processIndividualAnimal(
            ServerLevel level,
            PathfinderMob animal,
            long currentTime
    ) {
        UUID animalId = animal.getUUID();

        // Herd members use their herd's shared shelter search instead.
        if (HerdManager.getHerd(animal).isPresent()) {
            INDIVIDUAL_SHELTERS.remove(animalId);
            return;
        }

        IndividualShelterState state = INDIVIDUAL_SHELTERS.computeIfAbsent(
                animalId,
                ignored -> new IndividualShelterState(currentTime)
        );

        // Already found a shelter or settled at the fallback position.
        if (state.target != null) {
            return;
        }

        // Give up after five in-game minutes.
        if (currentTime - state.searchStartedAt >= MAX_SEARCH_TIME) {
            BlockPos restingPosition = animal.blockPosition();

            state.target = new ShelterTarget(
                    restingPosition,
                    Map.of(animalId, restingPosition),
                    false
            );

            return;
        }

        // Don't search every tick.
        if (currentTime < state.nextSearchTick) {
            return;
        }

        var profile = ShelterProfiles.get(animal.getType());

        if (profile.isEmpty()) {
            return;
        }

        // Search around this animal rather than a herd centre.
        var target = ShelterFinder.find(
                level,
                animal.position(),
                profile.get(),
                List.of(animalId)
        );

        if (target.isPresent()) {
            state.target = target.get();
        } else {
            state.nextSearchTick = currentTime + SEARCH_RETRY_DELAY;
        }
    }

    public static Optional<ShelterTarget> getShelterTarget(Entity entity) {
        if (!(entity instanceof PathfinderMob animal)) {
            return Optional.empty();
        }

        // Herd members use their shared target.
        Optional<Herd> herd = HerdManager.getHerd(animal);

        if (herd.isPresent()) {
            return herd.get().shelterTarget();
        }

        // Otherwise, look for an individual animal target.
        IndividualShelterState state = INDIVIDUAL_SHELTERS.get(animal.getUUID());

        if (state != null) {
            return Optional.ofNullable(state.target);
        }

        return Optional.empty();
    }

    public static boolean isShelterSearchActive(Entity entity) {
        if (!(entity instanceof PathfinderMob animal)) {
            return false;
        }

        Optional<Herd> herd = HerdManager.getHerd(animal);

        if (herd.isPresent()) {
            return herd.get().isShelterActive();
        }

        return INDIVIDUAL_SHELTERS.containsKey(animal.getUUID());
    }
}