package com.innspectre.ecology.herd;

import java.util.EnumSet;
import java.util.Optional;

import com.innspectre.ecology.food.ForageExplorationManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps herd members from drifting too far away from their herd.
 *
 * The goal is deliberately weak:
 *
 * - animals can wander freely while inside their comfort radius
 * - it only activates once an animal is well outside that radius
 * - it aims somewhere inside the herd rather than at the exact centre
 * - a little random jitter prevents all members from piling onto one point
 *
 * Food exploration and shelter take precedence over cohesion.
 */
public class HerdCohesionGoal extends Goal {

    /**
     * The animal must be this far beyond its comfort radius before cohesion
     * starts pulling it back.
     */
    private static final double TRIGGER_FACTOR = 1.25;

    /**
     * The animal aims for this fraction of the comfort radius from the centre.
     */
    private static final double PULL_TO_FACTOR = 0.6;

    /**
     * Random sideways movement around the target.
     */
    private static final double JITTER = 2.0;

    /**
     * How frequently an animal checks whether it needs to return.
     */
    private static final int CHECK_DELAY = 20;

    private final PathfinderMob mob;
    private final double speed;

    private Vec3 target;
    private boolean pathFound;

    public HerdCohesionGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Don't perform the more expensive herd lookup every tick.
        if (mob.getRandom().nextInt(reducedTickDelay(CHECK_DELAY)) != 0) {
            return false;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        Optional<HerdProfile> profile = HerdProfiles.get(mob.getType());
        Optional<Herd> herd = HerdManager.getHerd(mob);

        if (profile.isEmpty() || herd.isEmpty()) {
            return false;
        }

        Herd currentHerd = herd.get();

        // A herd that is sheltering has its own movement behaviour.
        if (currentHerd.isShelterActive()) {
            return false;
        }

        // Deliberate food exploration is allowed to pull animals away
        // from the herd.
        if (ForageExplorationManager.isExploring(mob)) {
            return false;
        }

        Optional<Vec3> centre = HerdManager.centre(level, currentHerd);

        if (centre.isEmpty()) {
            return false;
        }

        double dx = mob.getX() - centre.get().x;
        double dz = mob.getZ() - centre.get().z;

        double distance = Math.sqrt(dx * dx + dz * dz);

        double comfort = profile.get().comfortFor(currentHerd.size());

        // Inside this radius the animal is completely free to wander.
        if (distance <= comfort * TRIGGER_FACTOR) {
            return false;
        }

        // Avoid division by zero, even though the distance check above
        // should make this practically impossible.
        if (distance < 0.001) {
            return false;
        }

        /*
         * Pull the animal towards the herd, but don't send it all the way
         * to the centre.
         *
         * This produces a softer flocking effect:
         *
         *       animal
         *          \
         *           \
         *            X  <- target
         *          [herd]
         *
         * rather than:
         *
         *       animal ---> exact centre
         */
        double pullTo = comfort * PULL_TO_FACTOR;

        double nx = dx / distance;
        double nz = dz / distance;

        double jitterX =
                (mob.getRandom().nextDouble() - 0.5)
                * 2.0
                * JITTER;

        double jitterZ =
                (mob.getRandom().nextDouble() - 0.5)
                * 2.0
                * JITTER;

        target = new Vec3(
                centre.get().x + nx * pullTo + jitterX,
                mob.getY(),
                centre.get().z + nz * pullTo + jitterZ
        );

        return true;
    }

    @Override
    public void start() {
        if (target == null) {
            pathFound = false;
            return;
        }

        pathFound = mob.getNavigation().moveTo(
                target.x,
                target.y,
                target.z,
                speed
        );
    }

    @Override
    public boolean canContinueToUse() {
        if (!pathFound || mob.getNavigation().isDone()) {
            return false;
        }

        Optional<Herd> herd = HerdManager.getHerd(mob);

        if (herd.isEmpty()) {
            return false;
        }

        Herd currentHerd = herd.get();

        if (currentHerd.isShelterActive()) {
            return false;
        }

        return !ForageExplorationManager.isExploring(mob);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();

        target = null;
        pathFound = false;
    }
}