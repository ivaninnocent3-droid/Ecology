package com.innspectre.ecology.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import com.innspectre.ecology.recognition.RecognitionManager;
import com.innspectre.ecology.recognition.RecognitionInteraction;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void ecology$onDamaged(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!cir.getReturnValue()) {
            return;
        }

        if (!(entity instanceof Animal animal)) {
            return;
        }

        if (!(source.getEntity() instanceof Player player)) {
            return;
        }

        // Only count direct player attacks, not arrows or other projectiles.
        if (source.getDirectEntity() != player) {
            return;
        }

        RecognitionManager.applyInteraction(
                animal,
                player.getUUID(),
                RecognitionInteraction.HIT
        );
    }
}