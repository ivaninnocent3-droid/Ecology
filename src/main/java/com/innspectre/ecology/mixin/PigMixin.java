package com.innspectre.ecology.mixin;

import com.innspectre.ecology.temperament.TemperamentGoals;
import com.innspectre.ecology.food.ForageExplorationGoal;
import com.innspectre.ecology.food.SeekFoodGoal;
import com.innspectre.ecology.herd.HerdCohesionGoal;
import com.innspectre.ecology.shelter.SeekShelterGoal;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.pig.Pig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pig.class)
public abstract class PigMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ecology$addRunningAvoidanceGoal(CallbackInfo ci) {
        Pig pig = (Pig) (Object) this;
        GoalSelector goals = ((MobAccessor) pig).ecology$getGoalSelector();

        TemperamentGoals.addRunningAvoidanceGoal(pig, goals);
        goals.addGoal(4, new SeekShelterGoal(pig, 1.0));

        // Add ecology-based vegetation seeking.
        goals.addGoal(5, new SeekFoodGoal(
                pig,
                mob -> {
                    // No species-specific eating animation yet.
                },
                mob -> {
                    // No additional effect after eating.
                }
        ));
        goals.addGoal(6, new HerdCohesionGoal(pig, 1.0));
        goals.addGoal(5, new ForageExplorationGoal(pig, 1.0));
    }
}