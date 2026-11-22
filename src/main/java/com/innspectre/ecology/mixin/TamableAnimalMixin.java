package com.innspectre.ecology.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.innspectre.ecology.recognition.RecognitionManager;
import com.innspectre.ecology.recognition.RecognitionInteraction;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin {

    @Unique
    private float ecology$healthBeforeFeeding;

    @Inject(method = "feed", at = @At("HEAD"))
    private void ecology$beforeFeeding(
            Player player,
            InteractionHand hand,
            ItemStack itemStack,
            float healingFactor,
            float defaultHeal,
            CallbackInfo ci
    ) {
        TamableAnimal animal = (TamableAnimal) (Object) this;
        ecology$healthBeforeFeeding = animal.getHealth();
    }

    @Inject(method = "feed", at = @At("RETURN"))
    private void ecology$afterFeeding(
            Player player,
            InteractionHand hand,
            ItemStack itemStack,
            float healingFactor,
            float defaultHeal,
            CallbackInfo ci
    ) {
        TamableAnimal animal = (TamableAnimal) (Object) this;

        if (animal.level().isClientSide()) {
            return;
        }

        if (animal.getHealth() > ecology$healthBeforeFeeding) {
            RecognitionManager.applyInteraction(
                    animal,
                    player.getUUID(),
                    RecognitionInteraction.HEALING
            );
        }
    }
}