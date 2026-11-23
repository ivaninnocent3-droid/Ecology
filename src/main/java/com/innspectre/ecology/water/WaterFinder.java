
package com.innspectre.ecology.water;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.util.RandomSource;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import java.util.ArrayList;
import java.util.List;

public class WaterFinder {

    private static final int MAX_WATER_BODIES_TO_CHECK = 8;
    private static final int MAX_PATHFINDING_ATTEMPTS = 8;

    private record ShallowWaterCheck(
        boolean safe,
        int attemptsUsed
    ) {}

    public static List<BlockPos> findWaterSources(
        Level level,
        BlockPos centre,
        int radius
    ) {
        List<BlockPos> sources = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -radius; z <= radius; z++) {

                    BlockPos pos = centre.offset(x, y, z);

                    if (level.getBlockState(pos).is(Blocks.WATER)
                            && level.getFluidState(pos).getAmount() == 8) {
                        sources.add(pos);
                    }
                }
            }
        }

        return sources;
    }

    public static List<WaterBody> findWaterBodies(
        List<BlockPos> waterSources
    ) {
        List<WaterBody> waterBodies = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> sourceSet = new HashSet<>(waterSources);

        for (BlockPos source : waterSources) {
            if (visited.contains(source)) {
                continue;
            }

            Set<BlockPos> connectedBlocks = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();

            queue.add(source);
            visited.add(source);

            while (!queue.isEmpty()) {
                BlockPos current = queue.removeFirst();
                connectedBlocks.add(current);

                for (Direction direction : Direction.values()) {
                    BlockPos neighbour = current.relative(direction);

                    if (visited.contains(neighbour)) {
                        continue;
                    }

                    if (sourceSet.contains(neighbour)) {
                        visited.add(neighbour);
                        queue.addLast(neighbour);
                    }
                }
            }

            waterBodies.add(new WaterBody(List.copyOf(connectedBlocks)));
        }

        return waterBodies;
    }

    public static boolean isReachable(
        PathfinderMob animal,
        BlockPos waterPos
    ) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos drinkingPos = waterPos.relative(direction);

            if (animal.getNavigation().createPath(drinkingPos, 0) != null) {
                return true;
            }
        }

        return false;
    }

    public static List<DrinkingPosition> findAllDrinkingPositions(
        PathfinderMob animal,
        BlockPos centre,
        int radius
) {
        List<DrinkingPosition> positions = new ArrayList<>();

        for (BlockPos waterPos : findWaterSources(
                animal.level(), centre, radius
        )) {
            positions.addAll(findDrinkingPositions(animal, waterPos));
        }

        return positions;
    }


    public static List<BlockPos> findBankDrinkingPositions(
        PathfinderMob animal,
        BlockPos waterPos
) {
        List<BlockPos> positions = new ArrayList<>();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos drinkingPos = waterPos.relative(direction);

            var path = animal.getNavigation().createPath(drinkingPos, 0);

            if (path != null && path.canReach()) {
                positions.add(drinkingPos);
            }
        }

        return positions;
    }

    private static boolean isTallEnoughForShallowWater(PathfinderMob animal) {
        return animal.getBbHeight() > 1.0F;
    }

    
    public static ShallowWaterCheck hasSafeShallowWaterPosition(
            PathfinderMob animal,
            BlockPos waterPos,
            int maxPathfindingAttempts
    ) {
        if (!isTallEnoughForShallowWater(animal)) {
            return new ShallowWaterCheck(false, 0);
        }

        int attempts = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos exitPos = waterPos.relative(direction);

            if (!animal.level().getBlockState(exitPos).getFluidState().isEmpty()) {
                continue;
            }

            if (animal.level().getBlockState(exitPos.below()).isAir()) {
                continue;
            }

            if (attempts + 2 > maxPathfindingAttempts) {
                return new ShallowWaterCheck(false, attempts);
            }

            attempts += 2;

            var waterPath = animal.getNavigation().createPath(waterPos, 0);
            var exitPath = animal.getNavigation().createPath(exitPos, 0);

            if (waterPath != null && waterPath.canReach()
                    && exitPath != null && exitPath.canReach()) {
                return new ShallowWaterCheck(true, attempts);
            }
        }

        return new ShallowWaterCheck(false, attempts);
    }


    public static List<DrinkingPosition> findDrinkingPositions(
        PathfinderMob animal,
        BlockPos waterPos
    ) {
        List<DrinkingPosition> positions = new ArrayList<>();

        // Find positions on the bank
        for (BlockPos bankPos : findBankDrinkingPositions(animal, waterPos)) {
            positions.add(new DrinkingPosition(
                    bankPos,
                    waterPos,
                    DrinkingPosition.Type.BANK
            ));
        }

        // Check whether the animal can use shallow water
        if (isTallEnoughForShallowWater(animal)) {
            positions.add(new DrinkingPosition(
                    waterPos,
                    waterPos,
                    DrinkingPosition.Type.SHALLOW_WATER
            ));
        }

        return positions;
    }

    public static List<DrinkingPosition> findWaterBodyDrinkingPositions(
        PathfinderMob animal,
        WaterBody waterBody
    ) {
        List<DrinkingPosition> positions = new ArrayList<>();

        for (BlockPos waterPos : waterBody.getShoreline()) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos drinkingPos = waterPos.relative(direction);

                var blockState = animal.level().getBlockState(drinkingPos);

                if (!blockState.getFluidState().isEmpty()) {
                    continue;
                }

                positions.add(new DrinkingPosition(
                        drinkingPos,
                        waterPos,
                        DrinkingPosition.Type.BANK
                ));
            }

            if (isTallEnoughForShallowWater(animal)) {
                positions.add(new DrinkingPosition(
                        waterPos,
                        waterPos,
                        DrinkingPosition.Type.SHALLOW_WATER
                ));
            }
        }

        return positions;
    }

    
    public static List<DrinkingPosition> findAvailableDrinkingPositions(
            PathfinderMob animal,
            BlockPos centre,
            int radius,
            WaterCooldowns cooldowns
    ) {
        List<DrinkingPosition> candidates = new ArrayList<>();
        List<DrinkingPosition> positions = new ArrayList<>();

        long currentTime = animal.level().getGameTime();

        List<BlockPos> waterSources = findWaterSources(
                animal.level(), centre, radius
        );

        List<WaterBody> waterBodies = findWaterBodies(waterSources);

        waterBodies.sort(
                java.util.Comparator.comparingDouble(
                        body -> body.blocks().stream()
                                .mapToDouble(centre::distSqr)
                                .min()
                                .orElse(Double.MAX_VALUE)
                )
        );

        int checkedBodies = 0;

        for (WaterBody waterBody : waterBodies) {
            if (checkedBodies >= MAX_WATER_BODIES_TO_CHECK) {
                break;
            }

            BlockPos representative = waterBody.blocks().getFirst();

            if (cooldowns.isOnCooldown(representative, currentTime)) {
                continue;
            }

            candidates.addAll(
                    findWaterBodyDrinkingPositions(animal, waterBody)
            );

            checkedBodies++;
        }

        candidates.sort(
                java.util.Comparator.comparingDouble(
                        candidate -> centre.distSqr(candidate.position())
                )
        );

        int pathfindingAttempts = 0;

        for (DrinkingPosition candidate : candidates) {
            if (pathfindingAttempts >= MAX_PATHFINDING_ATTEMPTS) {
                break;
            }


            if (candidate.type() == DrinkingPosition.Type.BANK) {
                if (candidate.type() == DrinkingPosition.Type.BANK) {
                    pathfindingAttempts++;

                    var path = animal.getNavigation().createPath(
                            candidate.position(), 0
                    );

                    if (path != null && path.canReach()) {
                        positions.add(candidate);
                    }
                }
            } else {
                int remainingAttempts = MAX_PATHFINDING_ATTEMPTS - pathfindingAttempts;

                ShallowWaterCheck result = hasSafeShallowWaterPosition(
                        animal,
                        candidate.waterSource(),
                        remainingAttempts
                );

                pathfindingAttempts += result.attemptsUsed();

                if (result.safe()) {
                    positions.add(candidate);
                }
            }
        }

        return positions;
    }




    /**
     * Finds safe, reachable drinking positions around a particular known
     * water source.
     *
     * Unlike the general search, this does not search for other water bodies.
     */
    public static List<DrinkingPosition> findAvailableDrinkingPositionsAtSource(
            PathfinderMob animal,
            BlockPos waterPos
    ) {
        List<DrinkingPosition> positions = new ArrayList<>();

        // The source may have changed since the herd learned about it.
        if (!animal.level().getBlockState(waterPos).is(Blocks.WATER)
                || animal.level().getFluidState(waterPos).getAmount() != 8) {
            return positions;
        }

        // Check reachable positions on the bank.
        for (BlockPos bankPos : findBankDrinkingPositions(animal, waterPos)) {
            positions.add(new DrinkingPosition(
                    bankPos,
                    waterPos,
                    DrinkingPosition.Type.BANK
            ));
        }

        // Also allow shallow-water drinking where it is safe.
        if (isTallEnoughForShallowWater(animal)) {
            ShallowWaterCheck result = hasSafeShallowWaterPosition(
                    animal,
                    waterPos,
                    MAX_PATHFINDING_ATTEMPTS
            );

            if (result.safe()) {
                positions.add(new DrinkingPosition(
                        waterPos,
                        waterPos,
                        DrinkingPosition.Type.SHALLOW_WATER
                ));
            }
        }

        return positions;
    }




    public static DrinkingPosition chooseDrinkingPosition(
        List<DrinkingPosition> positions,
        BlockPos animalPos,
        RandomSource random
    ) {
        if (positions.isEmpty()) {
            return null;
        }

        double totalWeight = 0;

        for (DrinkingPosition position : positions) {
            double distance = Math.sqrt(
                    animalPos.distSqr(position.position())
            );

            totalWeight += 1.0 / (distance + 1.0);
        }

        double selection = random.nextDouble() * totalWeight;

        for (DrinkingPosition position : positions) {
            double distance = Math.sqrt(
                    animalPos.distSqr(position.position())
            );

            double weight = 1.0 / (distance + 1.0);
            selection -= weight;

            if (selection <= 0) {
                return position;
            }
        }

        return positions.getLast();
    }
}
