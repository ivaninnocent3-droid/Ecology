package com.innspectre.ecology.mixin;

import com.innspectre.ecology.temperament.TemperamentGoals;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.AbstractCow;

import com.innspectre.ecology.food.FoodSourceType;
import com.innspectre.ecology.food.FoodProfiles;
import com.innspectre.ecology.food.SeekFoodGoal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCow.class)
public abstract class CowMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ecology$addRunningAvoidanceGoal(CallbackInfo ci) {
        AbstractCow cow = (AbstractCow) (Object) this;
        GoalSelector goals = ((MobAccessor) cow).ecology$getGoalSelector();

        TemperamentGoals.addRunningAvoidanceGoal(cow, goals);

        // Add ecology-based grazing to animals whose diet includes vegetation.
        if (FoodProfiles.get(cow.getType())
                .filter(profile -> profile.accepts(FoodSourceType.VEGETATION))
                .isPresent()) {

            goals.addGoal(5, new SeekFoodGoal(
                    cow,
                    mob -> {
                        // Cows do not use the sheep eating animation.
                    },
                    mob -> {
                        // No additional species-specific effect after grazing.
                    }
            ));
        }
    }
}