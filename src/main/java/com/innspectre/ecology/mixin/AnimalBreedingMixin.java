package com.innspectre.ecology.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;

import com.innspectre.ecology.recognition.RecognitionManager;
import com.innspectre.ecology.recognition.RecognitionInteraction;

@Mixin(Animal.class)
public abstract class AnimalBreedingMixin {

    @Inject(method = "spawnChildFromBreeding", at = @At("HEAD"))
    private void ecology$onBreeding(
            ServerLevel level,
            Animal partner,
            CallbackInfo ci
    ) {
        Animal animal = (Animal) (Object) this;

        ServerPlayer firstPlayer = animal.getLoveCause();
        ServerPlayer secondPlayer = partner.getLoveCause();

        if (firstPlayer != null) {
            RecognitionManager.applyInteraction(
                    animal,
                    firstPlayer.getUUID(),
                    RecognitionInteraction.BREEDING
            );
        }

        if (secondPlayer != null) {
            RecognitionManager.applyInteraction(
                    partner,
                    secondPlayer.getUUID(),
                    RecognitionInteraction.BREEDING
            );
        }
    }
}