package com.innspectre.ecology.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.animal.wolf.Wolf;

import com.innspectre.ecology.food.LargePreyProfiles;
import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;


import com.innspectre.ecology.recognition.RecognitionManager;
import com.innspectre.ecology.recognition.RecognitionInteraction;
import com.innspectre.ecology.predation.PredationManager;

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

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void ecology$clearPredationKill(
            ServerLevel level,
            DamageSource source,
            CallbackInfo ci
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (PredationManager.isPredationKill(entity)) {
            PredationManager.clearPredationKill(entity);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void ecology$onPredationDeath(
            DamageSource source,
            CallbackInfo ci
    ) {
        LivingEntity prey = (LivingEntity) (Object) this;

        // Only reward wolves for killing supported large prey.
        if (!LargePreyProfiles.isLargePrey(prey.getType())) {
            return;
        }

        if (!(source.getEntity() instanceof Wolf wolf)) {
            return;
        }

        // Tamed wolves do not participate in independent hunting.
        if (wolf.isTame()) {
            return;
        }

        // Mark the prey so its meat can be filtered from its drops.
        PredationManager.markPredationKill(prey);

        // Restore some hunger to the successful hunter.
        NeedManager.change(wolf, NeedType.HUNGER, 30);
    }
}