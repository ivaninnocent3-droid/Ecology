package com.innspectre.ecology.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.innspectre.ecology.recognition.RecognitionManager;
import com.innspectre.ecology.recognition.RecognitionInteraction;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {

    @Unique
    private float ecology$healthBeforeFeeding;

    @Inject(method = "handleEating", at = @At("HEAD"))
    private void ecology$beforeFeeding(
            Player player,
            ItemStack itemStack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        ecology$healthBeforeFeeding = horse.getHealth();
    }

    @Inject(method = "handleEating", at = @At("RETURN"))
    private void ecology$afterFeeding(
            Player player,
            ItemStack itemStack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AbstractHorse horse = (AbstractHorse) (Object) this;

        if (horse.level().isClientSide()) {
            return;
        }

        if (cir.getReturnValue()
                && horse.getHealth() > ecology$healthBeforeFeeding) {
            RecognitionManager.applyInteraction(
                    horse,
                    player.getUUID(),
                    RecognitionInteraction.HEALING
            );
        }
    }
}