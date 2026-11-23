package com.innspectre.ecology.client.mixin;

import com.innspectre.ecology.animation.AnimalActionAccess;
import com.innspectre.ecology.client.animation.AnimalActionRenderStateAccess;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ecology$copyAction(
            LivingEntity entity,
            LivingEntityRenderState state,
            float partialTick,
            CallbackInfo ci
    ) {
        if (entity instanceof AnimalActionAccess action) {
            ((AnimalActionRenderStateAccess) state).ecology$setAction(
                    action.ecology$getAction(),
                    action.ecology$getActionProgress()
            );
        }
    }
}