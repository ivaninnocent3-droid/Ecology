package com.innspectre.ecology.food;

import java.util.List;

public class AnimalDiet {

    private final List<FoodSource> foodSources;

    public AnimalDiet(List<FoodSource> foodSources) {
        this.foodSources = List.copyOf(foodSources);
    }

    public List<FoodSource> getFoodSources() {
        return foodSources;
    }
}
