
package com.innspectre.ecology.temperament;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;

public final class AnimalThreatRules {

    private AnimalThreatRules() {
    }

    public static boolean shouldReact(
            PathfinderMob animal,
            Player player
    ) {
        AnimalTemperament temperament =
                AnimalTemperament.forEntity(animal);

        AnimalResponse response =
                AnimalResponse.getResponse(animal, player);

        return switch (response) {
            case FLEE -> true;

            case WARY ->
                    !player.isCrouching()
                    || animal.distanceToSqr(player) <= 9.0;

            case COMFORTABLE ->
                    (player.isSprinting()
                            && temperament.effectiveFearfulness(animal, player) >= 3)
                    || temperament.fleesFromApproachingPlayer(animal, player);
        };
    }
}
