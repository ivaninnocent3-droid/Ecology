package com.innspectre.ecology.recognition;

/**
 * Defines how different interactions affect an animal's
 * recognition of and relationship with a player.
 */
public enum RecognitionInteraction {

    FEEDING(3, 5),
    BREEDING(2, 3),
    HIT(5, -15),
    HEALING(4, 10);

    private final int recognitionStrength;
    private final int relationshipChange;

    RecognitionInteraction(int recognitionStrength, int relationshipChange) {
        this.recognitionStrength = recognitionStrength;
        this.relationshipChange = relationshipChange;
    }

    public int recognitionStrength() {
        return recognitionStrength;
    }

    public int relationshipChange() {
        return relationshipChange;
    }
}