
package com.innspectre.ecology.herd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Optional;

import com.innspectre.ecology.intelligence.AnimalIntelligence;
import com.innspectre.ecology.temperament.AnimalTemperament;
import com.innspectre.ecology.temperament.AnimalThreatRules;
import com.innspectre.ecology.temperament.Threat;
import com.innspectre.ecology.temperament.ThreatManager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Detects threats to herds and gradually spreads an alarm between members.
 *
 * All state is temporary and server-side. The original threat position is
 * retained so every animal has a consistent direction to flee from.
 */
public final class HerdAlarmTicker {

    private static final int DETECTION_INTERVAL = 20;
    private static final double EXTRA_DETECTION_DISTANCE = 12.0;

    private static final int PROPAGATION_INTERVAL = 4;
    private static final double ALARM_SPREAD_RADIUS = 8.0;
    private static final long ALARM_DURATION = 35L;
    private static final int INITIAL_RESPONDER_COUNT = 3;
    private static final long HERD_THREAT_MEMORY_TICKS = 30L;

    private static final Map<ServerLevel, Map<UUID, Alarm>> ACTIVE_ALARMS =
            new WeakHashMap<>();

    private static final class Alarm {
        private final UUID sourceId;
        private final Player source;
        private Vec3 threatPosition;
        private final Set<UUID> alerted = new HashSet<>();
        private final Set<UUID> initialResponders = new HashSet<>();

        private long nextPropagationTick;
        private long expiresAt;

        private Alarm(
                Player source,
                Vec3 threatPosition,
                long currentTick,
                List<PathfinderMob> firstResponders
        ) {
            this.sourceId = source.getUUID();
            this.source = source;
            this.threatPosition = threatPosition;
            this.nextPropagationTick = currentTick + PROPAGATION_INTERVAL;
            this.expiresAt = currentTick + ALARM_DURATION;
            for (PathfinderMob animal : firstResponders) {
                this.alerted.add(animal.getUUID());
                this.initialResponders.add(animal.getUUID());
            }
        }
    }

    private HerdAlarmTicker() {
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(HerdAlarmTicker::tick);
    }

    private static void tick(ServerLevel level) {
        long now = level.getGameTime();

        Map<UUID, Alarm> alarms = ACTIVE_ALARMS.computeIfAbsent(
                level,
                key -> new HashMap<>()
        );

        HerdRegistry registry = HerdRegistry.get(level);

        Set<UUID> expiredAlarms = new HashSet<>();

        // Process existing alarms first.
        alarms.entrySet().removeIf(entry -> {
            Alarm alarm = entry.getValue();

            Herd herd = registry.getHerd(entry.getKey()).orElse(null);

            if (herd == null) {
                return true;
            }

        // Only refresh and propagate while the player is actively approaching.
        boolean activelyThreatened = refreshAlarm(level, herd, alarm, now);

        if (now >= alarm.expiresAt) {
            expiredAlarms.add(entry.getKey());
            return true;
        }

        if (activelyThreatened && now >= alarm.nextPropagationTick) {
            propagate(level, herd, alarm, now);
            alarm.nextPropagationTick = now + PROPAGATION_INTERVAL;
        }

            return false;
        });

        // Only perform player detection periodically, staggered by herd ID.
        for (Herd herd : registry.herds()) {
            if (alarms.containsKey(herd.id())) {
                continue;
            }

        // Check immediately if this herd's previous alarm just expired.
        // Otherwise, retain the staggered detection schedule.
        if (!expiredAlarms.contains(herd.id())
                && Math.floorMod(herd.id().hashCode() + (int) now,
                        DETECTION_INTERVAL) != 0) {
            continue;
        }

        detectPlayer(level, herd, alarms, now);
        }
    }

    private static boolean refreshAlarm(
        ServerLevel level,
        Herd herd,
        Alarm alarm,
        long now
    ) {
        Player player = alarm.source;

        if (!player.isAlive()
                || player.level() != level
                || player.isSpectator()
                || player.isCreative()) {
            return false;
        }

        var centreOptional = HerdManager.centre(level, herd);

        if (centreOptional.isEmpty()) {
            return false;
        }

        Vec3 centre = centreOptional.get();

        double detectionRadius = herd.radius() + EXTRA_DETECTION_DISTANCE;

        double dx = player.getX() - centre.x;
        double dz = player.getZ() - centre.z;

        if (dx * dx + dz * dz > detectionRadius * detectionRadius) {
            return false;
        }

        // Work out whether the player is actually moving towards the herd.
        Vec3 towardHerd = new Vec3(-dx, 0, -dz).normalize();
        Vec3 movement = player.getDeltaMovement();

        double approachSpeed =
                movement.x * towardHerd.x
                + movement.z * towardHerd.z;

        // Don't refresh the alarm if the player has stopped or is moving away.
        if (approachSpeed <= 0.01) {
            return false;
        }

        alarm.threatPosition = player.position();
        alarm.expiresAt = now + ALARM_DURATION;

        return true;
    }

    private static void detectPlayer(
            ServerLevel level,
            Herd herd,
            Map<UUID, Alarm> alarms,
            long now
    ) {
        var centreOptional = HerdManager.centre(level, herd);
        if (centreOptional.isEmpty()) {
            return;
        }

        Vec3 centre = centreOptional.get();
        double detectionRadius = herd.radius() + EXTRA_DETECTION_DISTANCE;

        AABB searchArea = new AABB(centre, centre).inflate(detectionRadius);

        List<Player> nearbyPlayers = level.getEntitiesOfClass(
                Player.class,
                searchArea,
                player -> player.isAlive()
                        && !player.isSpectator()
                        && !player.isCreative()
        );

        for (Player player : nearbyPlayers) {
            double dx = player.getX() - centre.x;
            double dz = player.getZ() - centre.z;

            if (dx * dx + dz * dz > detectionRadius * detectionRadius) {
                continue;
            }

            List<PathfinderMob> firstResponders =
                    findInitialResponders(level, herd, player);

            if (firstResponders.isEmpty()) {
                continue;
            }

            Vec3 threatPosition = player.position();

            Alarm alarm = new Alarm(
                    player,
                    threatPosition,
                    now,
                    firstResponders
            );

            alarms.put(herd.id(), alarm);

            for (PathfinderMob animal : firstResponders) {
                ThreatManager.threaten(
                        animal,
                        new Threat(player.getUUID(), threatPosition, 1.0),
                        HERD_THREAT_MEMORY_TICKS
                );
            }

            return;
        }
    }

    public static Optional<Vec3> getCollectiveFleeDirection(
        PathfinderMob animal,
        UUID sourceId
    ) {
        if (!(animal.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }

        Herd herd = HerdManager.getHerd(animal).orElse(null);

        if (herd == null) {
            return Optional.empty();
        }

        Map<UUID, Alarm> alarms = ACTIVE_ALARMS.get(level);

        if (alarms == null) {
            return Optional.empty();
        }

        Alarm alarm = alarms.get(herd.id());

        if (alarm == null || !alarm.sourceId.equals(sourceId)) {
            return Optional.empty();
        }

        Vec3 sum = Vec3.ZERO;

        for (UUID responderId : alarm.initialResponders) {
            Entity entity = level.getEntity(responderId);

            if (!(entity instanceof PathfinderMob responder)
                    || !responder.isAlive()) {
                continue;
            }

            Vec3 awayFromPlayer = new Vec3(
                    responder.getX() - alarm.source.getX(),
                    0,
                    responder.getZ() - alarm.source.getZ()
            ).normalize();

            sum = sum.add(awayFromPlayer);
        }

        if (sum.lengthSqr() < 1.0E-6) {
            return Optional.empty();
        }

        return Optional.of(sum.normalize());
    }

    private static List<PathfinderMob> findInitialResponders(
            ServerLevel level,
            Herd herd,
            Player player
    ) {
        List<PathfinderMob> responders = new ArrayList<>();

        for (UUID memberId : herd.members()) {
            Entity entity = level.getEntity(memberId);

            if (!(entity instanceof PathfinderMob animal)
                    || !animal.isAlive()
                    || !AnimalTemperament.supports(animal)
                    || !AnimalThreatRules.shouldReact(animal, player)) {
                continue;
            }

            responders.add(animal);
        }

        responders.sort(
                java.util.Comparator.comparingDouble(
                        animal -> animal.distanceToSqr(player)
                )
        );

        return responders.stream()
                .limit(INITIAL_RESPONDER_COUNT)
                .toList();
    }

    private static void propagate(
            ServerLevel level,
            Herd herd,
            Alarm alarm,
            long now
    ) {
        List<PathfinderMob> alertedAnimals = new ArrayList<>();

        for (UUID memberId : alarm.alerted) {
            Entity entity = level.getEntity(memberId);

            if (entity instanceof PathfinderMob animal && animal.isAlive()) {
                alertedAnimals.add(animal);

                ThreatManager.threaten(
                        animal,
                        new Threat(
                                alarm.sourceId,
                                alarm.threatPosition,
                                1.0
                        ),
                        HERD_THREAT_MEMORY_TICKS
                );
            }
        }

        if (alertedAnimals.isEmpty()) {
            return;
        }

        List<PathfinderMob> newlyAlerted = new ArrayList<>();

        for (UUID memberId : herd.members()) {
            if (alarm.alerted.contains(memberId)) {
                continue;
            }

            Entity entity = level.getEntity(memberId);

            if (!(entity instanceof PathfinderMob animal)
                    || !animal.isAlive()
                    || !AnimalTemperament.supports(animal)) {
                continue;
            }

            if (!isNearAlertedAnimal(animal, alertedAnimals)) {
                continue;
            }

            if (alarm.source.isAlive()
                    && !AnimalThreatRules.shouldReact(animal, alarm.source)) {
                continue;
            }

            int learningRate =
                    AnimalIntelligence.forEntity(animal).learningRate();

            double reactionChance = 0.25 + learningRate * 0.15;

            if (animal.getRandom().nextDouble() >= reactionChance) {
                continue;
            }

            newlyAlerted.add(animal);
        }

        // Add them after the scan so one wave cannot spread multiple times in one tick.
        for (PathfinderMob animal : newlyAlerted) {
            alarm.alerted.add(animal.getUUID());

            ThreatManager.threaten(
                    animal,
                    new Threat(
                            alarm.sourceId,
                            alarm.threatPosition,
                            1.0
                    ),
                    HERD_THREAT_MEMORY_TICKS
            );
        }
    }

    private static boolean isNearAlertedAnimal(
            PathfinderMob animal,
            List<PathfinderMob> alertedAnimals
    ) {
        double spreadRadiusSqr = ALARM_SPREAD_RADIUS * ALARM_SPREAD_RADIUS;

        for (PathfinderMob alerted : alertedAnimals) {
            if (animal.distanceToSqr(alerted) <= spreadRadiusSqr) {
                return true;
            }
        }

        return false;
    }
}
