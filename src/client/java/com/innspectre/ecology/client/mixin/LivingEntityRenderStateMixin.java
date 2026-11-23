package com.innspectre.ecology.client.mixin;

import com.innspectre.ecology.client.animation.AnimalActionRenderStateAccess;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin
        implements AnimalActionRenderStateAccess {

    @Unique
    private byte ecology$action;

    @Unique
    private float ecology$actionProgress;

    @Override
    public void ecology$setAction(byte action, float progress) {
        ecology$action = action;
        ecology$actionProgress = progress;
    }

    @Override
    public byte ecology$getAction() {
        return ecology$action;
    }

    @Override
    public float ecology$getActionProgress() {
        return ecology$actionProgress;
    }
}