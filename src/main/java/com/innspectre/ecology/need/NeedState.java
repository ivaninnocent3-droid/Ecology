package com.innspectre.ecology.need;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record NeedState(
        int value,
        long lastUpdated,
        boolean paused,
        long decayProgress
) {
    public static final Codec<NeedState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("value")
                            .forGetter(NeedState::value),
                    Codec.LONG.fieldOf("last_updated")
                            .forGetter(NeedState::lastUpdated),
                    Codec.BOOL.optionalFieldOf("paused", false)
                            .forGetter(NeedState::paused),
                    Codec.LONG.optionalFieldOf("decay_progress", 0L)
                            .forGetter(NeedState::decayProgress)
            ).apply(instance, NeedState::new));

    // Keeps existing three-argument construction compatible.
    public NeedState(int value, long lastUpdated, boolean paused) {
        this(value, lastUpdated, paused, 0L);
    }
}