package com.innspectre.ecology.temperament;

import java.util.EnumSet;
import java.util.Optional;

import com.innspectre.ecology.herd.Herd;
import com.innspectre.ecology.herd.HerdManager;
import com.innspectre.ecology.herd.HerdAlarmTicker;
import com.innspectre.ecology.intelligence.AnimalIntelligence;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Makes an animal flee from its current threat.
 *
 * The direction is not simply "opposite the threat". Herd members influence
 * the direction, with more intelligent animals following the herd direction
 * more strongly.
 */
public class FleeGoal extends Goal {

    private static final double FLEE_DISTANCE = 14.0;
    private static final double MIN_DIRECTION_LENGTH = 0.001;

    private final PathfinderMob animal;
    private final double speed;

    private Vec3 target;

    public FleeGoal(PathfinderMob animal, double speed) {
        this.animal = animal;
        this.speed = speed;

        this.setFlags(EnumSet.of(
                Goal.Flag.MOVE,
                Goal.Flag.LOOK
        ));
    }

    @Override
    public boolean canUse() {
        Threat threat = ThreatManager.get(animal);

        if (threat == null) {
            return false;
        }

        target = chooseFleeTarget(threat);

        return target != null;
    }

    @Override
    public void start() {
        if (target != null) {
            animal.getNavigation().moveTo(
                    target.x,
                    target.y,
                    target.z,
                    speed
            );
        }
    }

    @Override
    public boolean canContinueToUse() {
        return ThreatManager.get(animal) != null;
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
        target = null;
    }

    @Override
    public void tick() {
        Threat threat = ThreatManager.get(animal);

        if (threat == null) {
            return;
        }

        if (animal.getNavigation().isDone()) {
            target = chooseFleeTarget(threat);

            if (target != null) {
                animal.getNavigation().moveTo(
                        target.x,
                        target.y,
                        target.z,
                        speed
                );
            }
        }
    }

    private Vec3 chooseFleeTarget(Threat threat) {
        Vec3 away = animal.position().subtract(threat.position());

        away = new Vec3(
                away.x,
                0,
                away.z
        );

        if (away.lengthSqr() < MIN_DIRECTION_LENGTH) {
            away = randomDirection(animal.getRandom());
        } else {
            away = away.normalize();
        }

        // Prefer the collective direction established by the initial responders.
        // Fall back to observing the movement of other alerted herd members.
        Vec3 herdDirection = HerdAlarmTicker
                .getCollectiveFleeDirection(animal, threat.sourceId())
                .orElseGet(() -> getHerdDirection(threat));

        double coherence = herdCoherence();

        Vec3 direction;

        if (herdDirection != null) {
            direction = away.scale(1.0 - coherence)
                    .add(herdDirection.scale(coherence));

            if (direction.lengthSqr() > MIN_DIRECTION_LENGTH) {
                direction = direction.normalize();
            } else {
                direction = away;
            }
        } else {
            direction = away;
        }

        // Add individual variation so the herd does not become
        // a perfectly synchronized line of animals.
        direction = addRandomVariation(
                direction,
                animal.getRandom(),
                1.0 - coherence
        );

        direction = direction.normalize();

        return new Vec3(
                animal.getX() + direction.x * FLEE_DISTANCE,
                animal.getY(),
                animal.getZ() + direction.z * FLEE_DISTANCE
        );
    }

    /**
     * Finds the average direction in which currently loaded herd members
     * are moving away from the threat.
     */
    private Vec3 getHerdDirection(Threat currentThreat) {
        Optional<Herd> herdOptional = HerdManager.getHerd(animal);

        if (herdOptional.isEmpty()) {
            return null;
        }

        Herd herd = herdOptional.get();

        if (!(animal.level() instanceof ServerLevel level)) {
            return null;
        }

        Vec3 sum = Vec3.ZERO;
        int count = 0;

        for (var memberId : herd.members()) {
            var member = level.getEntity(memberId);

            if (!(member instanceof PathfinderMob mob)
                    || !mob.isAlive()
                    || mob == animal) {
                continue;
            }

            Threat otherThreat = ThreatManager.get(mob);

            if (otherThreat == null
                    || !java.util.Objects.equals(
                            otherThreat.sourceId(),
                            currentThreat.sourceId()
                    )) {
                continue;
            }

            Vec3 movement = mob.getDeltaMovement();

            Vec3 horizontal = new Vec3(
                    movement.x,
                    0,
                    movement.z
            );

            if (horizontal.lengthSqr() < MIN_DIRECTION_LENGTH) {
                continue;
            }

            sum = sum.add(horizontal.normalize());
            count++;
        }

        if (count == 0 || sum.lengthSqr() < MIN_DIRECTION_LENGTH) {
            return null;
        }

        return sum.normalize();
    }

    /**
     * Converts AnimalIntelligence into herd-flight coherence.
     *
     * learningRate is currently the available intelligence measure, so we
     * deliberately map it into a 0..1 coherence value here.
     */
    private double herdCoherence() {
        int learningRate =
                AnimalIntelligence.forEntity(animal).learningRate();

        return Mth.clamp(
                learningRate / 10.0,
                0.0,
                1.0
        );
    }

    private static Vec3 addRandomVariation(
            Vec3 direction,
            RandomSource random,
            double amount
    ) {
        double angle = (random.nextDouble() * 2.0 - 1.0)
                * Math.toRadians(35.0)
                * amount;

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        return new Vec3(
                direction.x * cos - direction.z * sin,
                0,
                direction.x * sin + direction.z * cos
        );
    }

    private static Vec3 randomDirection(RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0;

        return new Vec3(
                Math.cos(angle),
                0,
                Math.sin(angle)
        );
    }
}