
package com.innspectre.ecology.food;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import com.innspectre.ecology.herd.Herd;
import com.innspectre.ecology.herd.HerdManager;
import com.innspectre.ecology.shelter.ShelterManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ForageExplorationManager {

    private static final long ASSESSMENT_INTERVAL = 200L;

    private static final int DESTINATION_ARRIVAL_DISTANCE = 6;
    private static final int DESTINATION_MEMORY_SIZE = 6;

    private static final int[] SEARCH_DISTANCES = {32, 48};
    private static final int PATHFINDING_ATTEMPTS = 4;

    private static final double MINIMUM_IMPROVEMENT = 2.0;

    private record StateKey(UUID id, boolean herd) {
    }

    private static final class ExplorationState {

        private boolean exploring;
        private BlockPos destination;

        private long nextAssessmentTick;
        private long lastAccessTick;

        private final Deque<BlockPos> recentDestinations =
                new ArrayDeque<>();
    }

    private record DestinationCandidate(
            BlockPos position,
            double score
    ) {
    }

    private static final Map<ServerLevel, Map<StateKey, ExplorationState>>
            STATES = new WeakHashMap<>();

    private ForageExplorationManager() {
    }

    private static Map<StateKey, ExplorationState> states(
            ServerLevel level
    ) {
        return STATES.computeIfAbsent(level, ignored -> new HashMap<>());
    }

    private static StateKey keyFor(
            PathfinderMob animal
    ) {
        Optional<Herd> herd = HerdManager.getHerd(animal);

        if (herd.isPresent()) {
            return new StateKey(herd.get().id(), true);
        }

        return new StateKey(animal.getUUID(), false);
    }

    private static ExplorationState getState(
            ServerLevel level,
            StateKey key
    ) {
        return states(level).computeIfAbsent(
                key,
                ignored -> new ExplorationState()
        );
    }

    /**
     * Returns the current exploration destination, if this animal
     * or its herd is actively exploring for vegetation.
     */
    public static Optional<BlockPos> getDestination(
            ServerLevel level,
            PathfinderMob animal
    ) {
        FoodProfile profile = FoodProfiles.get(animal.getType())
                .orElse(null);

        if (profile == null
                || !profile.accepts(FoodSourceType.VEGETATION)) {
            return Optional.empty();
        }

        StateKey key = keyFor(animal);
        ExplorationState state = getState(level, key);

        long currentTime = level.getGameTime();
        state.lastAccessTick = currentTime;

        // Shelter behaviour takes precedence.
        if (ShelterManager.isShelterSearchActive(animal)) {
            state.exploring = false;
            state.destination = null;
            return Optional.empty();
        }

        Optional<Herd> herd = HerdManager.getHerd(animal);

        int population = herd.map(Herd::size).orElse(1);

        BlockPos centre;

        if (herd.isPresent()) {
            var herdCentre = HerdManager.centre(level, herd.get());

            if (herdCentre.isEmpty()) {
                return Optional.empty();
            }

            centre = BlockPos.containing(herdCentre.get());
        } else {
            centre = animal.blockPosition();
        }

        // Has the herd or animal reached its destination?
        if (state.destination != null
                && centre.distSqr(state.destination)
                        <= DESTINATION_ARRIVAL_DISTANCE
                        * DESTINATION_ARRIVAL_DISTANCE) {

            rememberDestination(state, state.destination);
            state.destination = null;
            state.nextAssessmentTick = currentTime;
        }

        // Do not repeatedly scan the same area.
        if (currentTime < state.nextAssessmentTick) {
            return Optional.ofNullable(state.destination);
        }

        state.nextAssessmentTick =
                currentTime + ASSESSMENT_INTERVAL;

        double currentScore =
                VegetationQualityScanner.calculateScore(
                        level,
                        animal,
                        centre
                );

        if (VegetationQualityScanner.isAreaSustainable(
                currentScore,
                population
        )) {
            state.exploring = false;
            state.destination = null;
            return Optional.empty();
        }

        // The area is insufficient, so exploration is necessary.
        state.exploring = true;

        // Continue towards an existing destination.
        if (state.destination != null) {
            return Optional.of(state.destination);
        }

        state.destination = findDestination(
                level,
                animal,
                centre,
                currentScore,
                state
        );

        return Optional.ofNullable(state.destination);
    }

    /**
     * Whether the herd or individual is currently in food exploration mode.
     */
    public static boolean isExploring(Entity entity) {
        if (!(entity instanceof PathfinderMob animal)
                || !(animal.level() instanceof ServerLevel level)) {
            return false;
        }

        ExplorationState state = states(level).get(keyFor(animal));

        return state != null && state.exploring;
    }

    /**
     * Finds a better area to explore.
     *
     * Candidate areas are assessed first. Only the best few candidates
     * receive pathfinding checks.
     */
    private static BlockPos findDestination(
            ServerLevel level,
            PathfinderMob animal,
            BlockPos origin,
            double currentScore,
            ExplorationState state
    ) {
        List<DestinationCandidate> candidates = new ArrayList<>();

        double startingAngle =
                animal.getRandom().nextDouble() * Math.PI * 2.0;

        for (int distance : SEARCH_DISTANCES) {

            for (int direction = 0; direction < 8; direction++) {

                double angle = startingAngle
                        + direction * Math.PI / 4.0;

                int x = origin.getX()
                        + (int) Math.round(Math.cos(angle) * distance);

                int z = origin.getZ()
                        + (int) Math.round(Math.sin(angle) * distance);

                if (!level.hasChunkAt(BlockPos.containing(x, origin.getY(), z))) {
                    continue;
                }

                int y = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z
                );

                BlockPos candidate = new BlockPos(x, y, z);

                if (wasRecentlyVisited(state, candidate)) {
                    continue;
                }

                double score =
                        VegetationQualityScanner.calculateScore(
                                level,
                                animal,
                                candidate
                        );

                candidates.add(
                        new DestinationCandidate(candidate, score)
                );
            }
        }

        candidates.sort(
                Comparator.comparingDouble(
                        DestinationCandidate::score
                ).reversed()
        );

        int attempts = 0;

        for (DestinationCandidate candidate : candidates) {

            if (attempts >= PATHFINDING_ATTEMPTS) {
                break;
            }

            if (candidate.score()
                    < currentScore + MINIMUM_IMPROVEMENT
                    && attempts > 0) {
                continue;
            }

            attempts++;

            var path = animal.getNavigation().createPath(
                    candidate.position(),
                    0
            );

            if (path != null && path.canReach()) {
                return candidate.position();
            }
        }

        return null;
    }

    private static boolean wasRecentlyVisited(
            ExplorationState state,
            BlockPos candidate
    ) {
        for (BlockPos previous : state.recentDestinations) {
            if (previous.distSqr(candidate) < 24 * 24) {
                return true;
            }
        }

        return false;
    }

    private static void rememberDestination(
            ExplorationState state,
            BlockPos destination
    ) {
        state.recentDestinations.addLast(destination);

        while (state.recentDestinations.size()
                > DESTINATION_MEMORY_SIZE) {
            state.recentDestinations.removeFirst();
        }
    }
}
