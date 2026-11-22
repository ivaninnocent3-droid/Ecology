package com.innspectre.ecology.herd;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.innspectre.ecology.Ecology;

/**
 * Every herd in one level. This is the single source of truth for membership:
 * animals do not store their own herd id.
 *
 * Saved: herd id, species, member UUIDs.
 * Rebuilt on load (not saved): the animal-UUID -> herd-id lookup.
 *
 * Server thread only. Every method that changes anything calls setDirty(),
 * because data that isn't marked dirty is not saved.
 */
public class HerdRegistry extends SavedData {

    private static final Codec<HerdRegistry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Herd.CODEC.listOf().fieldOf("herds").forGetter(registry -> List.copyOf(registry.herds.values()))
    ).apply(instance, HerdRegistry::fromSaved));

    private static final SavedDataType<HerdRegistry> TYPE = new SavedDataType<>(
            Ecology.id("herds"),
            HerdRegistry::new,
            CODEC,
            null // data fixer, not needed yet
    );

    private final Map<UUID, Herd> herds = new HashMap<>();
    private final Map<UUID, UUID> herdByAnimal = new HashMap<>();

    public HerdRegistry() {
    }

    /** Each dimension has its own registry. */
    public static HerdRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ---- queries ----

    public Collection<Herd> herds() {
        return Collections.unmodifiableCollection(herds.values());
    }

    public Optional<Herd> getHerd(UUID herdId) {
        return Optional.ofNullable(herds.get(herdId));
    }

    public Optional<Herd> getHerdOf(UUID animal) {
        UUID herdId = herdByAnimal.get(animal);
        return herdId == null ? Optional.empty() : getHerd(herdId);
    }

    public boolean isInHerd(UUID animal) {
        return herdByAnimal.containsKey(animal);
    }

    // ---- changes ----

    /**
     * Records a player-caused incident for herd members that still need to receive it.
     */
    public void addPendingReaction(UUID herdId, UUID playerId, Collection<UUID> recipients) {
        Herd herd = herds.get(herdId);
        if (herd == null || recipients.isEmpty()) {
            return;
        }

        herd.addPendingReaction(playerId, recipients);
        setDirty();
    }

    /** Gets the members still waiting to receive this player's incident. */
    public List<UUID> getPendingReactions(UUID herdId, UUID playerId) {
        Herd herd = herds.get(herdId);
        return herd == null ? List.of() : herd.getPendingReactions(playerId);
    }

    /** Marks one member as having received the incident. */
    public void removePendingReaction(UUID herdId, UUID playerId, UUID recipient) {
        Herd herd = herds.get(herdId);
        if (herd == null) {
            return;
        }

        herd.removePendingReaction(playerId, recipient);

        // Once all pending reactions are processed, dissolve undersized herds.
        if (herd.size() < Herd.MIN_VIABLE_SIZE
                && !herd.hasPendingReactions()) {
            dissolve(herd);
        }

        setDirty();
    }

    /**
     * Creates a herd from animals that aren't in one yet. Returns empty if fewer than
     * Herd.MIN_VIABLE_SIZE are free. The caller checks that they are all the given species.
     */
    public Optional<Herd> createHerd(EntityType<?> species, Collection<UUID> founders) {
        List<UUID> free = founders.stream()
                .distinct()
                .filter(animal -> !herdByAnimal.containsKey(animal))
                .limit(Herd.MAX_SIZE)
                .toList();

        if (free.size() < Herd.MIN_VIABLE_SIZE) {
            return Optional.empty();
        }

        Herd herd = new Herd(UUID.randomUUID(), species, free);
        herds.put(herd.id(), herd);
        for (UUID animal : free) {
            herdByAnimal.put(animal, herd.id());
        }
        setDirty();
        return Optional.of(herd);
    }

    /** False if the herd doesn't exist, is full, or the animal is already in a herd. */
    public boolean addMember(UUID herdId, UUID animal) {
        Herd herd = herds.get(herdId);
        if (herd == null || herd.isFull() || herdByAnimal.containsKey(animal)) {
            return false;
        }
        herd.addMember(animal);
        herdByAnimal.put(animal, herdId);
        setDirty();
        return true;
    }

    /**
     * Removes an animal from its herd (it left, died, or moved dimension).
     * A herd that drops below the minimum size is dissolved.
     */
    public void removeMember(UUID animal) {
        UUID herdId = herdByAnimal.remove(animal);
        if (herdId == null) {
            return;
        }

        Herd herd = herds.get(herdId);
        if (herd != null) {
            herd.removeMember(animal);

            if (herd.size() < Herd.MIN_VIABLE_SIZE
                    && !herd.hasPendingReactions()) {
                dissolve(herd);
            }
        }

        setDirty();
    }

    public void dissolve(UUID herdId) {
        Herd herd = herds.get(herdId);
        if (herd != null) {
            dissolve(herd);
            setDirty();
        }
    }

    private void dissolve(Herd herd) {
        for (UUID animal : herd.members()) {
            herdByAnimal.remove(animal);
        }
        herds.remove(herd.id());
    }

    // ---- loading ----

    /** Rebuilds the registry from saved herds, dropping anything inconsistent. */
    private static HerdRegistry fromSaved(List<Herd> saved) {
        HerdRegistry registry = new HerdRegistry();
        for (Herd herd : saved) {
            // An animal listed in two herds (damaged data) stays in the first one only.
            for (UUID animal : List.copyOf(herd.members())) {
                if (registry.herdByAnimal.containsKey(animal)) {
                    herd.removeMember(animal);
                }
            }
            if (herd.size() < Herd.MIN_VIABLE_SIZE && !herd.hasPendingReactions()) {
                continue;
            }
            registry.herds.put(herd.id(), herd);
            for (UUID animal : herd.members()) {
                registry.herdByAnimal.put(animal, herd.id());
            }
        }
        return registry;
    }

    public List<UUID> getPendingReactionPlayerIds(UUID herdId) {
        Herd herd = herds.get(herdId);
        return herd == null ? List.of() : herd.getPendingReactionPlayerIds();
    }
}