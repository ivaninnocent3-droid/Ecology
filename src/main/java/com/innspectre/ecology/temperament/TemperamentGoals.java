
package com.innspectre.ecology.temperament;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;

public class TemperamentGoals {
    private static boolean shouldFleeFromPlayer(
        PathfinderMob animal,
        LivingEntity entity,
        AnimalTemperament temperament
    ) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        AnimalResponse response = AnimalResponse.getResponse(animal, player);

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

    public static void addRunningAvoidanceGoal(
        PathfinderMob animal,
        GoalSelector goalSelector
    ) {
        if (!AnimalTemperament.supports(animal)) {
            return;
        }

        AnimalTemperament temperament = AnimalTemperament.forEntity(animal);

        goalSelector.addGoal(1,
                new AvoidEntityGoal<Player>(
                        animal,
                        Player.class,
                        temperament.avoidanceDistance(),
                        1.0,
                        1.4,
                        player -> shouldFleeFromPlayer(animal, player, temperament)
                )
        );
    }
}
