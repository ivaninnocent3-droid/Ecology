package com.innspectre.ecology.herd;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

/**
 * One group of animals of a single species.
 *
 * Only the id, species and member UUIDs are saved. The centre is worked out from
 * loaded members at runtime and is never saved, because a saved position goes stale.
 *
 * All changes to membership go through HerdRegistry (the mutators are package-private),
 * so the registry can always mark itself dirty and keep its lookup tables in sync.
 */
public final class Herd {

    /** Placeholder values - tune later. */
    public static final int MAX_SIZE = 24;
    public static final int MIN_VIABLE_SIZE = 2;

    public static final Codec<Herd> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Herd::id),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("species").forGetter(Herd::species),
            UUIDUtil.CODEC.listOf().fieldOf("members").forGetter(herd -> List.copyOf(herd.members))
    ).apply(instance, Herd::new));

    private final UUID id;
    private final EntityType<?> species;
    private final Set<UUID> members = new LinkedHashSet<>();

    // Runtime only (not saved).
    private Vec3 centre;
    private double radius;
    private long centreUpdatedAt;

    Herd(UUID id, EntityType<?> species, Collection<UUID> members) {
        this.id = id;
        this.species = species;
        this.members.addAll(members);
    }

    public UUID id() {
        return id;
    }

    public EntityType<?> species() {
        return species;
    }

    public Set<UUID> members() {
        return Collections.unmodifiableSet(members);
    }

    public int size() {
        return members.size();
    }

    public boolean contains(UUID animal) {
        return members.contains(animal);
    }

    public boolean isFull() {
        return members.size() >= MAX_SIZE;
    }

    /** The cached centre of the loaded members, if it has been calculated. */
    public Optional<Vec3> centre() {
        return Optional.ofNullable(centre);
    }

    /**
     * Horizontal distance from the centre to the farthest loaded member, as of the last
     * centre calculation. Runtime only. 0 until the first calculation.
     */
    public double radius() {
        return radius;
    }

    public boolean centreIsStale(long gameTime, long maxAgeTicks) {
        // No centre yet counts as stale. (Don't start centreUpdatedAt at Long.MIN_VALUE:
        // subtracting it overflows and the centre would never be calculated.)
        return centre == null || gameTime - centreUpdatedAt >= maxAgeTicks;
    }

    // ---- package-private: only HerdRegistry / HerdManager change these ----

    boolean addMember(UUID animal) {
        return members.add(animal);
    }

    boolean removeMember(UUID animal) {
        return members.remove(animal);
    }

    void setCentre(Vec3 centre, double radius, long gameTime) {
        this.centre = centre;
        this.radius = radius;
        this.centreUpdatedAt = gameTime;
    }
}