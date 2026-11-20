package com.innspectre.ecology.herd;

/**
 * How one species behaves around herds. Distances are in blocks, chances are per check
 * (an animal is checked once every HerdTicker.CHECK_INTERVAL_TICKS ticks).
 *
 * joinRange / maxJoinChance      - how far from a herd's centre an animal can still join it,
 *                                  and the chance when it is right next to the centre
 * formRange / formChance         - when no herd is in range, herdless animals within formRange
 *                                  of each other can start a new herd
 * comfortRadius / comfortPerMember - inside this distance of the centre an animal never leaves;
 *                                  bigger herds spread out, so it grows with herd size
 * leaveRange / maxLeaveChance    - at this distance from the centre the leave chance is at its maximum
 * rejoinCooldownTicks            - how long an animal that left can't rejoin the same herd
 */
public record HerdProfile(
        double joinRange,
        double maxJoinChance,
        double formRange,
        double formChance,
        double comfortRadius,
        double comfortPerMember,
        double leaveRange,
        double maxLeaveChance,
        int rejoinCooldownTicks
) {

    public HerdProfile {
        // The leave range must sit beyond the comfort radius, or the leave formula breaks.
        if (leaveRange <= comfortRadius) {
            throw new IllegalArgumentException("leaveRange must be greater than comfortRadius");
        }
    }

    /** The comfort radius for a herd of the given size. */
    public double comfortFor(int herdSize) {
        return comfortRadius + comfortPerMember * herdSize;
    }
}