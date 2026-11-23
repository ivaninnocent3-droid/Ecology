package com.innspectre.ecology.client.mixin;

import com.innspectre.ecology.animation.AnimalActionEvents;
import com.innspectre.ecology.client.animation.AnimalActionRenderStateAccess;

import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.util.Mth;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdultChickenModel.class)
public abstract class AdultChickenModelMixin {

    @Shadow
    @Final
    private ModelPart head;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void ecology$animateAction(
            ChickenRenderState state,
            CallbackInfo ci
    ) {
        AnimalActionRenderStateAccess action =
                (AnimalActionRenderStateAccess) state;

        float progress = action.ecology$getActionProgress();

        if (progress <= 0.0F) {
            return;
        }

        if (action.ecology$getAction() == AnimalActionEvents.EATING) {
            head.xRot += progress * (
                    0.06F
                    + Mth.sin(state.ageInTicks * 0.85F) * 0.09F
            );
        } else if (action.ecology$getAction()
                == AnimalActionEvents.DRINKING) {
            head.xRot += progress * (
                    0.20F
                    + Mth.sin(state.ageInTicks * 0.20F) * 0.02F
            );
        }
    }
}