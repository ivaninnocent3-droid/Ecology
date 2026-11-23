package com.innspectre.ecology.shelter;

public record ShelterProfile(
        int searchRadius,
        int areaRadius,
        int verticalSearchRange,
        int minimumCapacity
) {
    public ShelterProfile {
        if (searchRadius < 1 || areaRadius < 1
                || verticalSearchRange < 0 || minimumCapacity < 1) {
            throw new IllegalArgumentException("Invalid shelter profile");
        }
    }
}