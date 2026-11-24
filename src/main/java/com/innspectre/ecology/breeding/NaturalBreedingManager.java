
package com.innspectre.ecology.breeding;

import java.util.List;

import com.mojang.serialization.Codec;

import com.innspectre.ecology.Ecology;
import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

public final class NaturalBreedingManager {

    private static final int REQUIRED_NEED_LEVEL = 85;

    private static final double PARTNER_RANGE = 6.0;
    private static final double PARTNER_RANGE_SQUARED =
            PARTNER_RANGE * PARTNER_RANGE;

    private static final long CHECK_INTERVAL = 200L;
    private static final float BREEDING_CHANCE = 0.005F;

    private static final long BREEDING_COOLDOWN = 48_000L;

    private static final AttachmentType<Long> BREEDING_COOLDOWN_ATTACHMENT =
            AttachmentRegistry.create(
                    Ecology.id("natural_breeding_cooldown"),
                    builder -> builder.persistent(Codec.LONG)
            );

    private NaturalBreedingManager() {
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(
                NaturalBreedingManager::tick
        );
    }

    private static void tick(ServerLevel level) {
        long gameTime = level.getGameTime();

        if (gameTime % CHECK_INTERVAL != 0) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Animal animal)) {
                continue;
            }

            if (!canAttemptBreeding(animal, gameTime)) {
                continue;
            }

            AABB searchArea = animal.getBoundingBox().inflate(PARTNER_RANGE);

            List<Animal> partners = level.getEntitiesOfClass(
                    Animal.class,
                    searchArea,
                    candidate ->
                            candidate != animal
                            && candidate.getType() == animal.getType()
                            && candidate.getUUID().compareTo(animal.getUUID()) > 0
                            && candidate.distanceToSqr(animal)
                                    <= PARTNER_RANGE_SQUARED
                            && canAttemptBreeding(candidate, gameTime)
            );

            if (partners.isEmpty()) {
                continue;
            }

            Animal partner = partners.get(
                    animal.getRandom().nextInt(partners.size())
            );

            if (animal.getRandom().nextFloat() >= BREEDING_CHANCE) {
                continue;
            }

            // Enter vanilla love mode. The normal breeding goals
            // will handle mating and offspring creation.
            animal.setInLove(null);
            partner.setInLove(null);
        }
    }

    private static boolean canAttemptBreeding(
            Animal animal,
            long gameTime
    ) {
        if (!animal.isAlive()
                || animal.isBaby()
                || animal.isInLove()
                || !NeedManager.hasTrackedNeeds(animal)) {
            return false;
        }

        if (gameTime < getCooldownUntil(animal)) {
            return false;
        }

        return NeedManager.get(animal, NeedType.HUNGER)
                        >= REQUIRED_NEED_LEVEL
                && NeedManager.get(animal, NeedType.THIRST)
                        >= REQUIRED_NEED_LEVEL;
    }

    private static long getCooldownUntil(Animal animal) {
        Long cooldown = animal.getAttached(
                BREEDING_COOLDOWN_ATTACHMENT
        );

        return cooldown == null ? 0L : cooldown;
    }

    /**
     * Called after vanilla successfully creates offspring.
     */
    public static void onSuccessfulBreeding(
            Animal animal,
            Animal partner,
            long gameTime
    ) {
        long cooldownUntil = gameTime + BREEDING_COOLDOWN;

        animal.setAttached(
                BREEDING_COOLDOWN_ATTACHMENT,
                cooldownUntil
        );

        partner.setAttached(
                BREEDING_COOLDOWN_ATTACHMENT,
                cooldownUntil
        );
    }
}
