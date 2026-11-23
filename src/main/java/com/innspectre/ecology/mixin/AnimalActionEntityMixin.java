package com.innspectre.ecology.mixin;

import com.innspectre.ecology.animation.AnimalActionAccess;
import com.innspectre.ecology.animation.AnimalActionEvents;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class AnimalActionEntityMixin implements AnimalActionAccess {

    @Unique
    private byte ecology$action = AnimalActionEvents.NONE;

    @Unique
    private float ecology$actionProgress;

    @Unique
    private boolean ecology$actionStopping;

    @Override
    public byte ecology$getAction() {
        return ecology$action;
    }

    @Override
    public float ecology$getActionProgress() {
        return ecology$actionProgress;
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void ecology$receiveActionEvent(byte id, CallbackInfo ci) {
        if (id == AnimalActionEvents.START_EATING) {
            ecology$action = AnimalActionEvents.EATING;
            ecology$actionStopping = false;
        } else if (id == AnimalActionEvents.STOP_EATING
                && ecology$action == AnimalActionEvents.EATING) {
            ecology$actionStopping = true;
        } else if (id == AnimalActionEvents.START_DRINKING) {
            ecology$action = AnimalActionEvents.DRINKING;
            ecology$actionStopping = false;
        } else if (id == AnimalActionEvents.STOP_DRINKING
                && ecology$action == AnimalActionEvents.DRINKING) {
            ecology$actionStopping = true;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ecology$updateActionProgress(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        if (!entity.level().isClientSide()) {
            return;
        }

        if (ecology$actionStopping) {
            ecology$actionProgress = Math.max(
                    0.0F,
                    ecology$actionProgress - 0.15F
            );

            if (ecology$actionProgress <= 0.0F) {
                ecology$action = AnimalActionEvents.NONE;
                ecology$actionStopping = false;
            }
        } else if (ecology$action != AnimalActionEvents.NONE) {
            ecology$actionProgress = Math.min(
                    1.0F,
                    ecology$actionProgress + 0.15F
            );
        }
    }
}