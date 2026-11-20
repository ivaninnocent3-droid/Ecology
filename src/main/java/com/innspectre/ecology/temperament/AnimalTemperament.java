
package com.innspectre.ecology.temperament;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.chicken.Chicken;
import com.innspectre.ecology.recognition.RecognitionManager;
import com.innspectre.ecology.recognition.RecognitionState;

import net.minecraft.world.entity.player.Player;

public enum AnimalTemperament {

    CHICKEN(4, 12),
    SHEEP(3, 8),
    COW(1, 3),
    PIG(2, 5),
    WOLF(2, 5);


    private static final int RUNNING_FEAR_THRESHOLD = 3;
    private static final int SNEAKING_REQUIRED_THRESHOLD = 5;
    private static final int APPROACH_FEAR_THRESHOLD = 5;

    private final int fearfulness;
    private final float avoidanceDistance;

    AnimalTemperament(int fearfulness, float avoidanceDistance) {
        this.fearfulness = fearfulness;
        this.avoidanceDistance = avoidanceDistance;
    }

    public int fearfulness() {
        return fearfulness;
    }

    public float avoidanceDistance() {
        return avoidanceDistance;
    }

    public boolean fleesFromRunningPlayer() {
        return fearfulness >= RUNNING_FEAR_THRESHOLD;
    }

    public boolean requiresSneakingToApproach() {
        return fearfulness >= SNEAKING_REQUIRED_THRESHOLD;
    }

    public boolean fleesFromApproachingPlayer(Entity animal, Player player) {
        return effectiveFearfulness(animal, player) >= APPROACH_FEAR_THRESHOLD;
    }

    public double effectiveFearfulness(Entity animal, Player player) {

        if (player == null) {
            return fearfulness;
        }

        RecognitionState recognition =
                RecognitionManager.get(animal, player.getUUID());

        double recognitionFactor = recognition.value() / 100.0;
        double relationshipFactor = recognition.relationship() / 100.0;

        double adjustment = 2.0 * recognitionFactor * relationshipFactor;

        return Math.max(1.0, Math.min(5.0, fearfulness - adjustment));
    }

    public static AnimalTemperament forEntity(Entity entity) {
        if (entity instanceof Chicken) return CHICKEN;
        if (entity instanceof Sheep) return SHEEP;
        if (entity instanceof Cow) return COW;
        if (entity instanceof Pig) return PIG;
        if (entity instanceof Wolf) return WOLF;

        return SHEEP;
    }

    public static boolean supports(Entity entity) {
        return entity instanceof Chicken
                || entity instanceof Sheep
                || entity instanceof Cow
                || entity instanceof Pig
                || entity instanceof Wolf;
    }
}
