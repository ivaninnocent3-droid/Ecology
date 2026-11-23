package com.innspectre.ecology.mixin;

import com.innspectre.ecology.predation.PredationManager;


import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(
        method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void ecology$removeMeatFromPredationDrops(
            ServerLevel level,
            ItemStack stack,
            CallbackInfoReturnable<ItemEntity> cir
    ) {
        Entity entity = (Entity) (Object) this;

        if (entity instanceof LivingEntity livingEntity
                && PredationManager.isPredationKill(livingEntity)
                && stack.is(ItemTags.MEAT)) {
            cir.setReturnValue(null);
        }
    }
}