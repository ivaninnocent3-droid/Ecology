package com.innspectre.ecology.need;

/**
 * A need an animal can have: its id, its maximum value and how quickly it drains.
 * Adding thirst later is one more line here.
 */
public enum NeedType {

    // 1 point every 1,200 ticks (60 seconds). Lower this (e.g. 20L) while testing.
    HUNGER("hunger", 100, 1200L);

    private final String id;
    private final int max;
    private final long ticksPerPoint;

    NeedType(String id, int max, long ticksPerPoint) {
        this.id = id;
        this.max = max;
        this.ticksPerPoint = ticksPerPoint;
    }

    public String id() {
        return id;
    }

    public int max() {
        return max;
    }

    public long ticksPerPoint() {
        return ticksPerPoint;
    }
}