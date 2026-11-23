
package com.innspectre.ecology.food;

import java.util.List;

public record FoodProfile(
    DietType diet,
    List<FoodSourceType> foodSources
) {
    public static FoodProfile sheep() {
        return new FoodProfile(
            DietType.HERBIVORE,
            List.of(FoodSourceType.VEGETATION)
        );
    }

    public static FoodProfile cow() {
        return new FoodProfile(
            DietType.HERBIVORE,
            List.of(FoodSourceType.VEGETATION)
        );
    }

    public static FoodProfile chicken() {
        return new FoodProfile(
            DietType.OMNIVORE,
            List.of(
                FoodSourceType.VEGETATION,
                FoodSourceType.SMALL_PREY
            )
        );
    }

    public static FoodProfile pig() {
        return new FoodProfile(
            DietType.OMNIVORE,
            List.of(
                FoodSourceType.VEGETATION,
                FoodSourceType.SMALL_PREY
            )
        );
    }

    public static FoodProfile wolf() {
        return new FoodProfile(
            DietType.CARNIVORE,
            List.of(
                FoodSourceType.DROPPED_ITEMS,
                FoodSourceType.SMALL_PREY,
                FoodSourceType.LARGE_PREY
            )
        );
    }

    public boolean accepts(FoodSourceType source) {
        return foodSources.contains(source);
    }
}
