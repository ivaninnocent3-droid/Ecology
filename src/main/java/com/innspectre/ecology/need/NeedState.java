
package com.innspectre.ecology.need;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Stores the value of a need, when it was last updated,
 * and whether its progression is currently paused.
 */
public record NeedState(int value, long lastUpdated, boolean paused) {

    public static final Codec<NeedState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("value").forGetter(NeedState::value),
            Codec.LONG.fieldOf("last_updated").forGetter(NeedState::lastUpdated),
            Codec.BOOL.optionalFieldOf("paused", false).forGetter(NeedState::paused)
    ).apply(instance, NeedState::new));
}
