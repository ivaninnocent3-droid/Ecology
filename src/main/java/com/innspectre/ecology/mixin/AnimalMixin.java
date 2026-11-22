package com.innspectre.ecology.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import com.innspectre.ecology.recognition.RecognitionManager;
import com.innspectre.ecology.recognition.RecognitionInteraction;

@Mixin(Mob.class)
public abstract class AnimalMixin {

    @Inject(method = "usePlayerItem", at = @At("HEAD"))
    private void ecology$onAnimalFed(
            Player player,
            InteractionHand hand,
            ItemStack stack,
            CallbackInfo ci
    ) {
        Mob mob = (Mob) (Object) this;

        if (!(mob instanceof Animal animal)) {
            return;
        }

        if (mob.level().isClientSide()) {
            return;
        }

        if (!animal.isFood(stack)) {
            return;
        }

        RecognitionManager.applyInteraction(
                animal,
                player.getUUID(),
                RecognitionInteraction.FEEDING
        );
    }
}