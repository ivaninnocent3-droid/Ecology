
package com.innspectre.ecology.attraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.innspectre.ecology.herd.Herd;

import net.minecraft.server.level.ServerLevel;

public final class AttractionManager {

    private static final long SIGNAL_DURATION = 40L;

    public enum Type {
        FOOD,
        SOCIAL
    }

    public record Signal(
            UUID playerId,
            UUID sourceId,
            Type type,
            long expiresAt
    ) {}

    private record SignalKey(UUID herdId, UUID playerId) {}

    private static final Map<ServerLevel, Map<SignalKey, Signal>> SIGNALS =
            new WeakHashMap<>();

    private AttractionManager() {}

    public static void publish(
            ServerLevel level,
            Herd herd,
            UUID playerId,
            UUID sourceId,
            Type type
    ) {
        long expiry = level.getGameTime() + SIGNAL_DURATION;

        Map<SignalKey, Signal> signals =
                SIGNALS.computeIfAbsent(level, key -> new HashMap<>());

        SignalKey signalKey = new SignalKey(herd.id(), playerId);

        signals.put(signalKey, new Signal(
                playerId,
                sourceId,
                type,
                expiry
        ));
    }

    public static List<Signal> getActiveSignals(
            ServerLevel level,
            Herd herd
    ) {
        Map<SignalKey, Signal> signals = SIGNALS.get(level);

        if (signals == null) {
            return List.of();
        }

        long now = level.getGameTime();
        List<Signal> active = new ArrayList<>();

        Iterator<Map.Entry<SignalKey, Signal>> iterator =
                signals.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<SignalKey, Signal> entry = iterator.next();
            SignalKey key = entry.getKey();
            Signal signal = entry.getValue();

            if (now >= signal.expiresAt()
                    || level.getPlayerByUUID(signal.playerId()) == null) {
                iterator.remove();
                continue;
            }

            if (key.herdId().equals(herd.id())) {
                active.add(signal);
            }
        }

        return active;
    }

    public static boolean isActive(
            ServerLevel level,
            Herd herd,
            UUID playerId,
            Type type
    ) {
        return getActiveSignals(level, herd).stream()
                .anyMatch(signal ->
                        signal.playerId().equals(playerId)
                                && signal.type() == type
                );
    }

    public static void removeSource(UUID sourceId) {
        for (Map<SignalKey, Signal> signals : SIGNALS.values()) {
            signals.values().removeIf(
                    signal -> signal.sourceId().equals(sourceId)
            );
        }
    }
}
