package com.innspectre.ecology.need;

/**
 * A need an animal can have: its id, its maximum value and how quickly it drains.
 * Adding thirst later is one more line here.
 */

public enum NeedType {

    HUNGER("hunger", 100, 70, 1200L),
    THIRST("thirst", 100, 70, 1200L);

    private final String id;
    private final int max;
    private final int initialValue;
    private final long ticksPerPoint;

    NeedType(String id, int max, int initialValue, long ticksPerPoint) {
        this.id = id;
        this.max = max;
        this.initialValue = initialValue;
        this.ticksPerPoint = ticksPerPoint;
    }

    public String id() {
        return id;
    }

    public int max() {
        return max;
    }

    public int initialValue() {
        return initialValue;
    }

    public long ticksPerPoint() {
        return ticksPerPoint;
    }
}
