package com.innspectre.ecology.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.innspectre.ecology.recognition.RecognitionManager;
import com.innspectre.ecology.AnimalEcologyGoals;
import com.innspectre.ecology.food.FoodProfiles;
import com.innspectre.ecology.food.ForageExplorationGoal;
import com.innspectre.ecology.food.SeekFoodGoal;
import com.innspectre.ecology.herd.HerdCohesionGoal;
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


    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ecology$addHerdGoals(CallbackInfo ci) {
        AbstractHorse horse = (AbstractHorse) (Object) this;

        GoalSelector goals =
                ((MobAccessor) horse).ecology$getGoalSelector();

        // Only horses with a registered Ecology food profile
        // participate in the new food and water behaviours.
        if (FoodProfiles.get(horse.getType()).isPresent()) {

            AnimalEcologyGoals.addSharedGoals(horse, goals);

            goals.addGoal(5, new SeekFoodGoal(
                    horse,
                    mob -> {
                        // No horse-specific eating animation yet.
                    },
                    mob -> {
                        // No additional effect after grazing.
                    }
            ));

            goals.addGoal(5, new ForageExplorationGoal(horse, 1.0));
        }

        goals.addGoal(6, new HerdCohesionGoal(horse, 1.0));
    }

}