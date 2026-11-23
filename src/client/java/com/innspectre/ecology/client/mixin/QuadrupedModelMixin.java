package com.innspectre.ecology.client.mixin;

import com.innspectre.ecology.animation.AnimalActionEvents;
import com.innspectre.ecology.client.animation.AnimalActionRenderStateAccess;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QuadrupedModel.class)
public abstract class QuadrupedModelMixin {

    @Shadow
    @Final
    protected ModelPart head;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void ecology$animateAction(
            LivingEntityRenderState state,
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
                    0.75F
                    + Mth.sin(state.ageInTicks * 0.55F) * 0.06F
            );
        } else if (action.ecology$getAction()
                == AnimalActionEvents.DRINKING) {
            head.xRot += progress * (
                    1.0F
                    + Mth.sin(state.ageInTicks * 0.20F) * 0.04F
            );
        }
    }
}