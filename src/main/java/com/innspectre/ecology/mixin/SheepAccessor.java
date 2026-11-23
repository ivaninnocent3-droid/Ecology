package com.innspectre.ecology.mixin;

import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.animal.sheep.Sheep;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Sheep.class)
public interface SheepAccessor {

    @Accessor("eatBlockGoal")
    EatBlockGoal ecology$getEatBlockGoal();
}