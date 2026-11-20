
package com.innspectre.ecology.mixin;

import com.innspectre.ecology.temperament.AnimalTemperament;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TemptGoal.class)
public abstract class TemptGoalMixin {

    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    protected Player player;

    @Inject(method = "canScare", at = @At("HEAD"), cancellable = true)
    private void ecology$applyTemperament(CallbackInfoReturnable<Boolean> cir) {

        if (!AnimalTemperament.supports(mob)) {
            return;
        }

        AnimalTemperament temperament = AnimalTemperament.forEntity(mob);

        if (player == null) {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(
                temperament.effectiveFearfulness(mob, player) >= 5
                        && !player.isShiftKeyDown()
        );
    }
}
