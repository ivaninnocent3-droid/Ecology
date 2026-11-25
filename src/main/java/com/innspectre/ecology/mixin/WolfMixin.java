
package com.innspectre.ecology.mixin;

import com.innspectre.ecology.temperament.TemperamentGoals;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.wolf.Wolf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.innspectre.ecology.herd.HerdCohesionGoal;
import com.innspectre.ecology.predation.WolfHuntTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import com.innspectre.ecology.AnimalEcologyGoals;


@Mixin(Wolf.class)
public abstract class WolfMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ecology$addRunningAvoidanceGoal(CallbackInfo ci) {
        Wolf wolf = (Wolf) (Object) this;

        GoalSelector goals = ((MobAccessor) wolf).ecology$getGoalSelector();
        AnimalEcologyGoals.addSharedGoals(wolf, goals);
        GoalSelector targetGoals = ((MobAccessor) wolf).ecology$getTargetSelector();

        TemperamentGoals.addRunningAvoidanceGoal(wolf, goals);

        // Remove vanilla natural animal hunting.
        targetGoals.removeAllGoals(goal ->
                goal instanceof NonTameRandomTargetGoal<?>
                && ((NearestAttackableTargetGoalAccessor) goal).ecology$getTargetType() == Animal.class
        );

        // Add hunger-driven hunting.
        targetGoals.addGoal(5, new WolfHuntTargetGoal(wolf));
        goals.addGoal(6, new HerdCohesionGoal(wolf, 1.0));
    }
}
