
package com.innspectre.ecology.mixin;

import com.innspectre.ecology.temperament.TemperamentGoals;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.chicken.Chicken;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public abstract class ChickenMixin {

    @Shadow
    protected GoalSelector goalSelector;

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ecology$addRunningAvoidanceGoal(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;

        TemperamentGoals.addRunningAvoidanceGoal(chicken, this.goalSelector);
    }
}
