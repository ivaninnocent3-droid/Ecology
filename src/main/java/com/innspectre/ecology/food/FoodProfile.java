package com.innspectre.ecology.food;

import java.util.List;

public record FoodProfile(
    List<FoodSourceType> foodSources
) {
    public static FoodProfile sheep() {
        return new FoodProfile(
            List.of(
                FoodSourceType.VEGETATION
            )
        );
    }

    public static FoodProfile wolf() {
        return new FoodProfile(
            List.of(
                FoodSourceType.DROPPED_ITEMS,
                FoodSourceType.PREY
            )
        );
    }
}