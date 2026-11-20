package com.innspectre.ecology.water;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.innspectre.ecology.need.NeedManager;
import com.innspectre.ecology.need.NeedType;

/**
 * A thirsty animal that hasn't found water goes looking for it.
 *
 * It walks in short legs (about 14 blocks), each one roughly continuing the last heading, so
 * it covers ground in a wandering line instead of circling. It never knows where water is.
 * At the end of every leg it simply looks around as normal, and the existing SeekWaterGoal
 * (a higher priority than this goal) takes over the moment water comes within its range,
 * so water found along the way is used.
 *
 * Two soft influences on where the next leg goes, both deliberately weak:
 *  - a short memory of places it already searched, which it steers away from
 *  - sometimes (HINT_CHANCE) a faint pull towards directions where, ~40 blocks out,
 *    the biome is a river or ocean. It's a hunch, not a location, and it is ignored
 *    on about half the legs and drowned out by random noise.
 *
 * It only walks through chunks that are already loaded.
 *
 * Priority: lower than SeekWaterGoal (so found water wins) and higher than cohesion and
 * wandering (so a thirsty animal can leave its herd's patch to search).
 */
public class WaterExplorationGoal extends Goal {

    /**
     * Start exploring when thirst is below this. Set it to the same value that makes
     * SeekWaterGoal start looking, or exploring will start too late or too early.
     */
    private static final int EXPLORE_BELOW = 50;

    /** Placeholder values - tune later. */
    private static final double LEG_LENGTH = 14.0;
    private static final int CANDIDATES = 6;
    private static final double MAX_TURN = Math.toRadians(70);
    private static final int MEMORY_SIZE = 8;
    private static final double HINT_DISTANCE = 40.0;
    private static final double HINT_CHANCE = 0.5;
    private static final double HINT_WEIGHT = 0.6;
    private static final double NOISE = 0.5;
    private static final int CHECK_DELAY = 20;

    private final PathfinderMob mob;
    private final double speed;

    /** Where legs ended recently, so the animal doesn't keep searching the same area. */
    private final Deque<Vec3> searched = new ArrayDeque<>();

    private double heading;
    private boolean hasHeading;
    private Vec3 target;
    private boolean pathFound;

    public WaterExplorationGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Cheapest check first: this is evaluated for every sheep.
        if (mob.getRandom().nextInt(reducedTickDelay(CHECK_DELAY)) != 0) {
            return false;
        }
        if (!isThirsty()) {
            forgetSearch();
            return false;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        if (!hasHeading) {
            heading = mob.getRandom().nextDouble() * Math.PI * 2.0;
            hasHeading = true;
        }

        target = chooseNextLeg(level);
        return target != null;
    }

    @Override
    public void start() {
        pathFound = mob.getNavigation().moveTo(target.x, target.y, target.z, speed);
    }

    @Override
    public boolean canContinueToUse() {
        return pathFound && !mob.getNavigation().isDone() && isThirsty();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();

        if (pathFound) {
            searched.addLast(mob.position());
            while (searched.size() > MEMORY_SIZE) {
                searched.removeFirst();
            }
        } else {
            // Couldn't walk that way (water, cliff...): pick a fresh heading next time.
            hasHeading = false;
        }

        target = null;
        pathFound = false;
    }

    private boolean isThirsty() {
        return NeedManager.get(mob, NeedType.THIRST) < EXPLORE_BELOW;
    }

    private void forgetSearch() {
        searched.clear();
        hasHeading = false;
    }

    /** Picks the best of a few headings near the current one, or null if none are usable. */
    private Vec3 chooseNextLeg(ServerLevel level) {
        RandomSource random = mob.getRandom();
        Vec3 origin = mob.position();
        boolean useHints = random.nextDouble() < HINT_CHANCE;

        Vec3 best = null;
        double bestAngle = heading;
        double bestScore = -Double.MAX_VALUE;

        for (int i = 0; i < CANDIDATES; i++) {
            double angle = heading + (random.nextDouble() * 2.0 - 1.0) * MAX_TURN;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            double x = origin.x + dx * LEG_LENGTH;
            double z = origin.z + dz * LEG_LENGTH;
            BlockPos legPos = BlockPos.containing(x, origin.y, z);
            if (!level.hasChunkAt(legPos)) {
                continue; // never walk into unloaded chunks
            }

            double score = random.nextDouble() * NOISE;
            score -= revisitPenalty(x, z);
            if (useHints) {
                score += HINT_WEIGHT * hint(level, origin.x + dx * HINT_DISTANCE, origin.y, origin.z + dz * HINT_DISTANCE);
            }

            if (score > bestScore) {
                bestScore = score;
                bestAngle = angle;
                double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
                best = new Vec3(x, y, z);
            }
        }

        if (best == null) {
            hasHeading = false; // nothing usable this way; turn around next time
            return null;
        }

        heading = bestAngle;
        return best;
    }

    /** Bigger the closer a candidate leg end is to somewhere already searched. */
    private double revisitPenalty(double x, double z) {
        double radius = LEG_LENGTH * 1.5;
        double penalty = 0.0;
        for (Vec3 place : searched) {
            double dx = x - place.x;
            double dz = z - place.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < radius) {
                penalty += 1.0 - distance / radius;
            }
        }
        return penalty;
    }

    /** 1 if the spot looks like river or ocean country, else 0. A hunch, not a location. */
    private double hint(ServerLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN) ? 1.0 : 0.0;
    }
}