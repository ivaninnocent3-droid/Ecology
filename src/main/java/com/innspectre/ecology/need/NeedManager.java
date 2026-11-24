package com.innspectre.ecology.need;

import java.util.EnumMap;
import java.util.Map;

import com.innspectre.ecology.Ecology;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public class NeedManager {

    private static final long NORMAL_RATE = 4L;
    private static final long SHELTER_RATE = 1L;

    private static final Map<NeedType, AttachmentType<NeedState>> ATTACHMENTS =
            new EnumMap<>(NeedType.class);

    static {
        for (NeedType type : NeedType.values()) {
            ATTACHMENTS.put(type, AttachmentRegistry.create(
                    Ecology.id("need_" + type.id()),
                    builder -> builder.persistent(NeedState.CODEC)
            ));
        }
    }

    public static void initialize() {
    }
    public static boolean hasTrackedNeeds(Entity entity) {
        return entity.hasAttached(ATTACHMENTS.get(NeedType.HUNGER))
                && entity.hasAttached(ATTACHMENTS.get(NeedType.THIRST));
    }

    public static void ensureStarted(Entity entity, NeedType type) {
        if (isExempt(entity)) {
            return;
        }

        AttachmentType<NeedState> attachment = ATTACHMENTS.get(type);

        if (!entity.hasAttached(attachment)) {
            entity.setAttached(
                    attachment,
                    new NeedState(type.initialValue(),
                            now(entity), false, 0L)
            );
        }
    }

    public static int get(Entity entity, NeedType type) {
        if (isExempt(entity)) {
            return type.max();
        }

        NeedState state = entity.getAttached(ATTACHMENTS.get(type));

        if (state == null) {
            return type.initialValue();
        }

        return state.value();
    }

    public static void set(Entity entity, NeedType type, int value) {
        if (isExempt(entity)) {
            return;
        }

        int clamped = Math.clamp(value, 0, type.max());

        entity.setAttached(
                ATTACHMENTS.get(type),
                new NeedState(clamped, now(entity), false, 0L)
        );
    }

    /**
     * Advances both need clocks.
     *
     * Normal time contributes four progress units per tick.
     * Properly sheltered time contributes one progress unit per tick.
     */
    public static void advance(Entity entity, boolean sheltered) {
        if (isExempt(entity)) {
            return;
        }

        for (NeedType type : NeedType.values()) {
            advance(entity, type, sheltered);
        }
    }

    private static void advance(
            Entity entity,
            NeedType type,
            boolean sheltered
    ) {
        AttachmentType<NeedState> attachment = ATTACHMENTS.get(type);
        NeedState state = entity.getAttached(attachment);

        if (state == null) {
            return;
        }

        long currentTime = now(entity);
        long elapsed = currentTime - state.lastUpdated();

        if (elapsed <= 0) {
            return;
        }

        // Paused needs do not accumulate decay progress.
        if (state.paused()) {
            entity.setAttached(
                    attachment,
                    new NeedState(
                            state.value(),
                            currentTime,
                            true,
                            state.decayProgress()
                    )
            );
            return;
        }

        long rate = sheltered ? SHELTER_RATE : NORMAL_RATE;

        long progress = state.decayProgress() + elapsed * rate;
        long progressPerPoint = type.ticksPerPoint() * NORMAL_RATE;

        long pointsLost = progress / progressPerPoint;
        long remainingProgress = progress % progressPerPoint;

        int newValue = (int) Math.max(0, state.value() - pointsLost);

        entity.setAttached(
                attachment,
                new NeedState(
                        newValue,
                        currentTime,
                        false,
                        remainingProgress
                )
        );
    }

    public static void pause(Entity entity, NeedType type) {
        if (isExempt(entity)) {
            return;
        }

        int currentValue = get(entity, type);

        entity.setAttached(
                ATTACHMENTS.get(type),
                new NeedState(currentValue, now(entity), true, 0L)
        );
    }

    public static void resume(Entity entity, NeedType type) {
        if (isExempt(entity)) {
            return;
        }

        AttachmentType<NeedState> attachment = ATTACHMENTS.get(type);
        NeedState state = entity.getAttached(attachment);

        if (state == null || !state.paused()) {
            return;
        }

        entity.setAttached(
                attachment,
                new NeedState(
                        state.value(),
                        now(entity),
                        false,
                        state.decayProgress()
                )
        );
    }

    public static void change(Entity entity, NeedType type, int amount) {
        set(entity, type, get(entity, type) + amount);
    }

    private static long now(Entity entity) {
        return entity.level().getGameTime();
    }

    private static boolean isExempt(Entity entity) {
        return entity instanceof Wolf wolf && wolf.isTame();
    }
}