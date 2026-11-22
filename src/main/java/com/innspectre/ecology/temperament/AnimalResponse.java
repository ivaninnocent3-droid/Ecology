package com.innspectre.ecology.temperament;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import com.innspectre.ecology.recognition.RecognitionManager;
import com.innspectre.ecology.recognition.RecognitionState;

public enum AnimalResponse {

    COMFORTABLE,
    WARY,
    FLEE;

    private static final int WARY_RELATIONSHIP = -40;
    private static final int FLEE_RELATIONSHIP = -70;

    private static final int WARY_RECOGNITION = 40;
    private static final int FLEE_RECOGNITION = 10;

    public static AnimalResponse getResponse(Entity animal, Player player) {

        if (player == null) {
            return COMFORTABLE;
        }

        RecognitionState recognition =
                RecognitionManager.get(animal, player.getUUID());

        int relationship = recognition.relationship();
        int familiarity = recognition.value();

        if (relationship <= FLEE_RELATIONSHIP
                && familiarity > FLEE_RECOGNITION) {
            return FLEE;
        }

        if (relationship <= WARY_RELATIONSHIP
                && familiarity < WARY_RECOGNITION) {
            return WARY;
        }

        return COMFORTABLE;
    }
}