package com.innspectre.ecology.recognition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Stores an animal's recognition of a player,
 * when it was last updated, and their relationship.
 */
public record RecognitionState(
        int value,
        long lastUpdated,
        int relationship
) {

    public static final Codec<RecognitionState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("value").forGetter(RecognitionState::value),
                    Codec.LONG.fieldOf("last_updated").forGetter(RecognitionState::lastUpdated),
                    Codec.INT.fieldOf("relationship").forGetter(RecognitionState::relationship)
            ).apply(instance, RecognitionState::new)
    );
}