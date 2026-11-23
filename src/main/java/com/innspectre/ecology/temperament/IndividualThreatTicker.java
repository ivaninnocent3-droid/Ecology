
package com.innspectre.ecology.temperament;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.innspectre.ecology.herd.HerdManager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class IndividualThreatTicker {

    private static final int DETECTION_INTERVAL = 10;
    private static final double DETECTION_RADIUS = 12.0;
    private static final long THREAT_MEMORY_TICKS = 30L;

    private static final Map<ServerLevel, Set<PathfinderMob>> TRACKED =
            new WeakHashMap<>();

    private IndividualThreatTicker() {
    }

    public static void register() {

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof PathfinderMob animal
                    && AnimalTemperament.supports(animal)) {

                TRACKED.computeIfAbsent(
                        level,
                        key -> new LinkedHashSet<>()
                ).add(animal);
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof PathfinderMob animal) {
                Set<PathfinderMob> tracked = TRACKED.get(level);

                if (tracked != null) {
                    tracked.remove(animal);
                }
            }
        });

        ServerTickEvents.END_LEVEL_TICK.register(
                IndividualThreatTicker::tick
        );
    }

    private static void tick(ServerLevel level) {

        Set<PathfinderMob> tracked = TRACKED.get(level);

        if (tracked == null || tracked.isEmpty()) {
            return;
        }

        long now = level.getGameTime();

        for (PathfinderMob animal : tracked) {

            if (!animal.isAlive()) {
                continue;
            }

            // Spread checks across ticks rather than checking
            // every animal simultaneously.
            if (Math.floorMod(
                    animal.getId() + (int) now,
                    DETECTION_INTERVAL
            ) != 0) {
                continue;
            }

            // Herd members are handled by HerdAlarmTicker.
            if (HerdManager.getHerd(animal).isPresent()) {
                continue;
            }

            detectThreat(level, animal);
        }
    }

    private static void detectThreat(
            ServerLevel level,
            PathfinderMob animal
    ) {

        AABB searchArea = animal.getBoundingBox()
                .inflate(DETECTION_RADIUS);

        var nearbyPlayers = level.getEntitiesOfClass(
                Player.class,
                searchArea,
                player -> player.isAlive()
                        && !player.isSpectator()
                        && !player.isCreative()
        );

        Player closestThreat = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player player : nearbyPlayers) {

            double distance = animal.distanceToSqr(player);

            if (distance > DETECTION_RADIUS * DETECTION_RADIUS) {
                continue;
            }

            if (!AnimalThreatRules.shouldReact(animal, player)) {
                continue;
            }

            if (distance < closestDistance) {
                closestDistance = distance;
                closestThreat = player;
            }
        }

        if (closestThreat == null) {
            return;
        }

        ThreatManager.threaten(
                animal,
                Threat.fromEntity(closestThreat, 1.0),
                THREAT_MEMORY_TICKS
        );
    }
}
