package com.innspectre.ecology.mixin;

import com.innspectre.ecology.temperament.TemperamentGoals;
import com.innspectre.ecology.AnimalEcologyGoals;
import com.innspectre.ecology.mixin.MobAccessor;
import com.innspectre.ecology.food.SeekFoodGoal;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.chicken.Chicken;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public abstract class ChickenMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ecology$addRunningAvoidanceGoal(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;
        GoalSelector goals = ((MobAccessor) chicken).ecology$getGoalSelector();

        TemperamentGoals.addRunningAvoidanceGoal(chicken, goals);
        AnimalEcologyGoals.addSharedGoals(chicken, goals);

        // Add ecology-based vegetation seeking.
        goals.addGoal(5, new SeekFoodGoal(
                chicken,
                mob -> {
                    // No species-specific eating animation yet.
                },
                mob -> {
                    // No additional effect after eating.
                }
        ));
    }
}