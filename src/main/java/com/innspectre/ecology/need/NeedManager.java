package com.innspectre.ecology.need;

import java.util.EnumMap;
import java.util.Map;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;

import com.innspectre.ecology.Ecology;
import net.minecraft.world.entity.animal.wolf.Wolf;

public class NeedManager {

    // One saved attachment per need type, e.g. "ecology:need_hunger".
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
        // Loading this class registers the attachments above.
    }

    /** Starts tracking a need at full value, if the entity isn't being tracked yet. */
    public static void ensureStarted(Entity entity, NeedType type) {
        if (isExempt(entity)) {
            return;
        }
        AttachmentType<NeedState> attachment = ATTACHMENTS.get(type);
        if (!entity.hasAttached(attachment)) {
            entity.setAttached(attachment, new NeedState(type.initialValue(), now(entity), false));
        }
    }

    /** The current value, worked out from the saved value and the time that has passed. */
    public static int get(Entity entity, NeedType type) {
        if (isExempt(entity)) {
            return type.max();
        }
        NeedState state = entity.getAttached(ATTACHMENTS.get(type));

        if (state == null) {
            return type.initialValue();
        }

        if (state.paused()) {
            return state.value();
        }

        return decayed(state, type, now(entity)).value();
    }

    public static void set(Entity entity, NeedType type, int value) {
        if (isExempt(entity)) {
            return;
        }

        int clamped = Math.clamp(value, 0, type.max());
        entity.setAttached(ATTACHMENTS.get(type), new NeedState(clamped, now(entity), false));
    }

    public static void pause(Entity entity, NeedType type) {
        if (isExempt(entity)) {
            return;
        }
        int currentValue = get(entity, type);

        entity.setAttached(
            ATTACHMENTS.get(type),
            new NeedState(currentValue, now(entity), true)
        );
    }

    public static void resume(Entity entity, NeedType type) {
        if (isExempt(entity)) {
            return;
        }

        NeedState state = entity.getAttached(ATTACHMENTS.get(type));
        if (state == null || !state.paused()) {
            return;
        }

        entity.setAttached(
            ATTACHMENTS.get(type),
            new NeedState(state.value(), now(entity), false)
        );
    }

    public static void change(Entity entity, NeedType type, int amount) {
        set(entity, type, get(entity, type) + amount);
    }

    private static NeedState decayed(NeedState state, NeedType type, long now) {
        long points = (now - state.lastUpdated()) / type.ticksPerPoint();
        if (points <= 0) {
            return state;
        }
        int value = (int) Math.max(0, state.value() - points);
        // Advance by whole points only, so partial progress isn't lost.
        return new NeedState(
            value,
            state.lastUpdated() + points * type.ticksPerPoint(),
            state.paused()
        );
    }

    private static long now(Entity entity) {
        return entity.level().getGameTime();
    }

    private static boolean isExempt(Entity entity) {
        return entity instanceof Wolf wolf && wolf.isTame();
    }
}