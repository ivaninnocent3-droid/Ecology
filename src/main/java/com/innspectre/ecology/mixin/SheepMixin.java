package com.innspectre.ecology.mixin;

import com.innspectre.ecology.AnimalEcologyGoals;
import com.innspectre.ecology.herd.HerdCohesionGoal;
import com.innspectre.ecology.temperament.TemperamentGoals;
import com.innspectre.ecology.water.*;
import com.innspectre.ecology.food.SeekFoodGoal;

import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.sheep.Sheep;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sheep.class)
public abstract class SheepMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ecology$addEcologyGoals(CallbackInfo ci) {
        Sheep sheep = (Sheep) (Object) this;
        GoalSelector goals = ((MobAccessor) sheep).ecology$getGoalSelector();

        // Replace vanilla eating with our ecology-based food system.
        EatBlockGoal vanillaEating =
                ((SheepAccessor) sheep).ecology$getEatBlockGoal();

        goals.removeGoal(vanillaEating);

        goals.addGoal(5, new SeekFoodGoal(
                sheep,
                mob -> mob.level().broadcastEntityEvent(mob, (byte) 10),
                mob -> sheep.ate()
        ));

        TemperamentGoals.addRunningAvoidanceGoal(sheep, goals);
        goals.addGoal(2, new SeekWaterGoal(sheep));
        goals.addGoal(3, new WaterExplorationGoal(sheep, 1.0));
        goals.addGoal(5, new HerdCohesionGoal(sheep, 1.0));
    }
}