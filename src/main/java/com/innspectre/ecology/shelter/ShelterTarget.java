package com.innspectre.ecology.shelter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;

public final class ShelterTarget {

    private final BlockPos centre;
    private final Map<java.util.UUID, BlockPos> positions;
    private final boolean roofed;

    public ShelterTarget(
            BlockPos centre,
            Map<java.util.UUID, BlockPos> positions,
            boolean roofed
    ) {
        this.centre = centre.immutable();

        Map<java.util.UUID, BlockPos> copy = new LinkedHashMap<>();

        positions.forEach((id, pos) ->
                copy.put(id, pos.immutable())
        );

        this.positions = Collections.unmodifiableMap(copy);
        this.roofed = roofed;

        if (positions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Shelter needs at least one assigned position"
            );
        }
    }

    public BlockPos centre() {
        return centre;
    }

    public Map<java.util.UUID, BlockPos> positions() {
        return positions;
    }

    public Optional<BlockPos> positionFor(java.util.UUID animalId) {
        return Optional.ofNullable(positions.get(animalId));
    }

    public boolean roofed() {
        return roofed;
    }
}