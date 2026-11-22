package com.innspectre.ecology.temperament;

import java.util.UUID;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * A temporary danger perceived by an animal.
 *
 * A threat has a source, a position and a strength. The source is optional because
 * some threats may come from something that is no longer directly available to the
 * animal, such as an alarm received from another herd member.
 */
public record Threat(
        UUID sourceId,
        Vec3 position,
        double strength
) {

    public static Threat fromEntity(Entity source, double strength) {
        return new Threat(
                source.getUUID(),
                source.position(),
                strength
        );
    }
}