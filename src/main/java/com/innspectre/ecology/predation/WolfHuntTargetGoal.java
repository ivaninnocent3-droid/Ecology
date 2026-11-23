
package com.innspectre.ecology.predation;

import com.innspectre.ecology.food.LargePreyProfiles;
import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class WolfHuntTargetGoal extends NearestAttackableTargetGoal<Animal> {

    private static final int HUNT_BELOW_HUNGER = 60;

    private final Wolf wolf;

    public WolfHuntTargetGoal(Wolf wolf) {
        super(
            wolf,
            Animal.class,
            10,
            false,
            false,
            (animal, level) -> LargePreyProfiles.isLargePrey(animal.getType())
        );

        this.wolf = wolf;
    }

    @Override
    public boolean canUse() {
        if (wolf.isTame()) {
            return false;
        }

        if (NeedManager.get(wolf, NeedType.HUNGER) >= HUNT_BELOW_HUNGER) {
            return false;
        }

        return super.canUse();
    }
}
