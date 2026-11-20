package com.innspectre.ecology.herd;

import java.util.EnumSet;
import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps herd members from drifting off: when a member is well outside its comfort radius,
 * it walks back towards the herd, then hands control back to the usual AI (wandering etc.).
 *
 * Inside the comfort radius this goal never runs, so animals wander as normal.
 *
 * Give it a lower priority than fleeing, eating and drinking (a higher number), and a higher
 * priority than normal wandering, so those things can still pull an animal away from the
 * herd, which is when the leave chance in HerdTicker gets its chance to fire.
 */
public class HerdCohesionGoal extends Goal {

    /** The goal only starts beyond comfort radius x this, which gives the leave chance room to fire. */
    private static final double TRIGGER_FACTOR = 1.25;

    /** The animal aims for this fraction of the comfort radius from the centre. */
    private static final double PULL_TO_FACTOR = 0.6;

    /** Random sideways wobble on the target, in blocks, so animals don't all aim for one spot. */
    private static final double JITTER = 2.0;

    /** On average, how often (in goal ticks) an animal that is too far out checks to start. */
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
        // Cheapest check first, since this runs for every herd member every tick.
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

        Optional<Vec3> centre = HerdManager.centre(level, herd.get());
        if (centre.isEmpty()) {
            return false;
        }

        double dx = mob.getX() - centre.get().x;
        double dz = mob.getZ() - centre.get().z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double comfort = profile.get().comfortFor(herd.get().size());

        if (distance <= comfort * TRIGGER_FACTOR) {
            return false;
        }

        // Aim for a point on the line from the centre towards this animal,
        // well inside the comfort radius, with a little random wobble.
        double pullTo = comfort * PULL_TO_FACTOR;
        double nx = dx / distance;
        double nz = dz / distance;
        double jitterX = (mob.getRandom().nextDouble() - 0.5) * 2.0 * JITTER;
        double jitterZ = (mob.getRandom().nextDouble() - 0.5) * 2.0 * JITTER;

        target = new Vec3(
                centre.get().x + nx * pullTo + jitterX,
                mob.getY(),
                centre.get().z + nz * pullTo + jitterZ);
        return true;
    }

    @Override
    public void start() {
        pathFound = mob.getNavigation().moveTo(target.x, target.y, target.z, speed);
    }

    @Override
    public boolean canContinueToUse() {
        // Stop when the walk is finished, no path was found, or the animal left the herd.
        return pathFound
                && !mob.getNavigation().isDone()
                && HerdManager.getHerd(mob).isPresent();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        target = null;
        pathFound = false;
    }
}