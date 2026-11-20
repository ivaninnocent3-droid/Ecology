
package com.innspectre.ecology.mixin;

import com.innspectre.ecology.temperament.TemperamentGoals;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.sheep.Sheep;
import com.innspectre.ecology.water.SeekWaterGoal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sheep.class)
public abstract class SheepMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ecology$addRunningAvoidanceGoal(CallbackInfo ci) {
        Sheep sheep = (Sheep) (Object) this;

        TemperamentGoals.addRunningAvoidanceGoal(
            sheep,
            ((MobAccessor) sheep).ecology$getGoalSelector()
        );
        ((MobAccessor) sheep).ecology$getGoalSelector().addGoal(
            2,
            new SeekWaterGoal(sheep)
        );
    }
}
