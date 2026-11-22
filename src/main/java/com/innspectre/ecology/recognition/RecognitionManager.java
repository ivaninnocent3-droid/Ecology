package com.innspectre.ecology.recognition;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import com.innspectre.ecology.Ecology;
import com.innspectre.ecology.herd.HerdManager;
import com.innspectre.ecology.intelligence.AnimalIntelligence;

public class RecognitionManager {

    // Stores recognition data for every player an animal has encountered.
    private static final AttachmentType<Map<String, RecognitionState>> RECOGNITION =
            AttachmentRegistry.create(
                    Ecology.id("recognition"),
                    builder -> builder.persistent(
                            Codec.unboundedMap(Codec.STRING, RecognitionState.CODEC)
                    )
            );

    // Recognition decay settings.
    private static final double BASE_DECAY_TICKS = 46800.0;
    private static final double DECAY_EXPONENT = 0.043;

    public static void initialize() {
        // Loading this class registers the attachment.
    }

    public static RecognitionState get(Entity entity, UUID playerId) {
        Map<String, RecognitionState> recognition =
                entity.getAttached(RECOGNITION);

        if (recognition == null) {
            return new RecognitionState(0, entity.level().getGameTime(), 0);
        }

        RecognitionState state = recognition.getOrDefault(
                playerId.toString(),
                new RecognitionState(0, entity.level().getGameTime(), 0)
        );

        return applyDecay(entity, state);
    }

    public static void set(Entity entity, UUID playerId, RecognitionState state) {
        Map<String, RecognitionState> recognition =
                entity.getAttached(RECOGNITION);

        Map<String, RecognitionState> updated = recognition == null
                ? new HashMap<>()
                : new HashMap<>(recognition);

        updated.put(playerId.toString(), new RecognitionState(
                Math.clamp(state.value(), 0, 100),
                state.lastUpdated(),
                Math.clamp(state.relationship(), -100, 100)
        ));

        entity.setAttached(RECOGNITION, updated);
    }

    public static void changeRecognition(Entity entity, UUID playerId, int amount) {
        RecognitionState state = get(entity, playerId);

        set(entity, playerId, new RecognitionState(
                state.value() + amount,
                entity.level().getGameTime(),
                state.relationship()
        ));
    }

    public static void changeRelationship(Entity entity, UUID playerId, int amount) {
        RecognitionState state = get(entity, playerId);

        set(entity, playerId, new RecognitionState(
                state.value(),
                state.lastUpdated(),
                state.relationship() + amount
        ));
    }

    public static void applyInteraction(
        Entity entity,
        UUID playerId,
        RecognitionInteraction interaction
        ) {
        RecognitionState state = get(entity, playerId);

        int learningRate = AnimalIntelligence.forEntity(entity).learningRate();

        int recognitionGain = interaction.recognitionStrength() * learningRate;

        int newRecognition = Math.clamp(
                state.value() + recognitionGain,
                0,
                100
        );

        int newRelationship = Math.clamp(
                state.relationship() + interaction.relationshipChange(),
                -100,
                100
        );

        set(entity, playerId, new RecognitionState(
                newRecognition,
                entity.level().getGameTime(),
                newRelationship
        ));

        // The rest of the herd learns a weaker version of the interaction.
        HerdManager.getHerd(entity).ifPresent(herd -> {
                if (!(entity.level() instanceof ServerLevel level)) {
                return;
                }

                for (UUID memberId : herd.members()) {
                if (memberId.equals(entity.getUUID())) {
                        continue;
                }

                Entity member = level.getEntity(memberId);

                // Only loaded, living members can learn about the interaction.
                if (member == null || !member.isAlive()) {
                        continue;
                }

                int memberLearningRate =
                        AnimalIntelligence.forEntity(member).learningRate();

                int sharedRecognitionGain =
                        Math.max(1, recognitionGain / 2) * memberLearningRate;

                int sharedRelationshipChange =
                        interaction.relationshipChange() / 2;

                RecognitionState memberState = get(member, playerId);

                set(member, playerId, new RecognitionState(
                        memberState.value() + sharedRecognitionGain,
                        level.getGameTime(),
                        memberState.relationship() + sharedRelationshipChange
                ));
                }
        });
    }

        /**
     * Calculates how much recognition has decayed since its last update.
     * Recognition takes progressively longer to decrease at higher values.
     */
    private static RecognitionState applyDecay(Entity entity, RecognitionState state) {
        long now = entity.level().getGameTime();

        if (state.value() <= 0 || now <= state.lastUpdated()) {
            return state;
        }

        int value = state.value();
        long lastUpdated = state.lastUpdated();

        while (value > 0) {
            long ticksPerPoint = (long) Math.ceil(
                    BASE_DECAY_TICKS * Math.exp(DECAY_EXPONENT * value)
            );

            if (now - lastUpdated < ticksPerPoint) {
                break;
            }

            lastUpdated += ticksPerPoint;
            value--;
        }

        return new RecognitionState(value, lastUpdated, state.relationship());
    }
}