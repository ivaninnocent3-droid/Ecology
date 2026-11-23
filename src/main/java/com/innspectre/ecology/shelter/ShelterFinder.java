package com.innspectre.ecology.shelter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

public final class ShelterFinder {

    private ShelterFinder() {
    }

    public static Optional<ShelterTarget> find(
            ServerLevel level,
            Vec3 origin,
            ShelterProfile profile,
            List<UUID> memberIds
    ) {
        if (memberIds.isEmpty()) {
            return Optional.empty();
        }

        BlockPos originPos = BlockPos.containing(origin);

        int searchRadius = profile.searchRadius();
        int areaRadius = profile.areaRadius();
        int scanRadius = searchRadius + areaRadius;

        List<BlockPos> validSpots = new ArrayList<>();

        for (int x = originPos.getX() - scanRadius;
             x <= originPos.getX() + scanRadius; x++) {

            for (int z = originPos.getZ() - scanRadius;
                 z <= originPos.getZ() + scanRadius; z++) {

                for (int y = originPos.getY() - profile.verticalSearchRange();
                     y <= originPos.getY() + profile.verticalSearchRange(); y++) {

                    BlockPos pos = new BlockPos(x, y, z);

                    if (isValidSpot(level, pos)) {
                        validSpots.add(pos);
                    }
                }
            }
        }

        ShelterTarget bestTarget = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        int requiredSpots = memberIds.size();

        for (BlockPos candidate : validSpots) {

            double dx = candidate.getX() + 0.5 - origin.x;
            double dz = candidate.getZ() + 0.5 - origin.z;

            double distanceSqr = dx * dx + dz * dz;

            if (distanceSqr > searchRadius * searchRadius) {
                continue;
            }

            List<BlockPos> spots = new ArrayList<>();

            for (BlockPos spot : validSpots) {
                int offsetX = spot.getX() - candidate.getX();
                int offsetZ = spot.getZ() - candidate.getZ();
                int offsetY = Math.abs(spot.getY() - candidate.getY());

                if (offsetX * offsetX + offsetZ * offsetZ
                        <= areaRadius * areaRadius
                        && offsetY <= 1) {

                    spots.add(spot);
                }
            }

            // Every animal needs its own position.
            if (spots.size() < requiredSpots) {
                continue;
            }

            double score = spots.size() * 1000.0 - distanceSqr * 10.0;

            if (score > bestScore) {
                bestScore = score;

                spots.sort(Comparator.comparingDouble(spot -> {
                    double sx = spot.getX() - candidate.getX();
                    double sz = spot.getZ() - candidate.getZ();
                    double sy = spot.getY() - candidate.getY();

                    return sx * sx + sy * sy + sz * sz;
                }));

                Map<UUID, BlockPos> assignments = new LinkedHashMap<>();

                for (int i = 0; i < memberIds.size(); i++) {
                    assignments.put(memberIds.get(i), spots.get(i));
                }

                bestTarget = new ShelterTarget(
                        candidate,
                        assignments,
                        true
                );
            }
        }

        return Optional.ofNullable(bestTarget);
    }

    public static boolean isValidSpot(ServerLevel level, BlockPos pos) {
        BlockPos floorPos = pos.below();
        BlockPos headPos = pos.above();
        BlockPos roofPos = pos.above(2);

        if (level.getChunk(
                pos.getX() >> 4,
                pos.getZ() >> 4,
                ChunkStatus.FULL,
                false
        ) == null) {
            return false;
        }

        var floor = level.getBlockState(floorPos);
        var feet = level.getBlockState(pos);
        var head = level.getBlockState(headPos);
        var roof = level.getBlockState(roofPos);

        return floor.isFaceSturdy(level, floorPos, Direction.UP)
                && floor.getFluidState().isEmpty()
                && feet.getCollisionShape(level, pos).isEmpty()
                && feet.getFluidState().isEmpty()
                && head.getCollisionShape(level, headPos).isEmpty()
                && head.getFluidState().isEmpty()
                && !roof.getCollisionShape(level, roofPos).isEmpty();
    }
}