
package com.innspectre.ecology.mixin;

import com.innspectre.ecology.temperament.TemperamentGoals;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.sheep.Sheep;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sheep.class)
public abstract class SheepMixin {

    @Shadow
    protected GoalSelector goalSelector;

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ecology$addRunningAvoidanceGoal(CallbackInfo ci) {
        Sheep sheep = (Sheep) (Object) this;

        TemperamentGoals.addRunningAvoidanceGoal(sheep, this.goalSelector);
    }
}
