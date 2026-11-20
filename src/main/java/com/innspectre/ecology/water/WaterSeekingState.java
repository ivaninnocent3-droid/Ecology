package com.innspectre.ecology.water;

public class WaterSeekingState {

    private DrinkingPosition target;
    private int stuckChecks;

    private static final long PROGRESS_CHECK_INTERVAL = 40L;
    private static final double MIN_PROGRESS = 0.5;

    private double lastDistance = Double.POSITIVE_INFINITY;
    private long lastProgressCheck;

    private boolean drinking;
    private int remainingDrinkAmount;
    private long nextDrinkDecisionTime;

    private long nextDrinkPointTime;

    public DrinkingPosition getTarget() {
        return target;
    }

    public void setTarget(DrinkingPosition target) {
        this.target = target;
        lastDistance = Double.POSITIVE_INFINITY;
        lastProgressCheck = 0;
        this.stuckChecks = 0;
    }

    public void clearTarget() {
        this.target = null;
        lastDistance = Double.POSITIVE_INFINITY;
        lastProgressCheck = 0;
        this.stuckChecks = 0;
    }

    public int getStuckChecks() {
        return stuckChecks;
    }

    public void incrementStuckChecks() {
        stuckChecks++;
    }

    public boolean checkProgress(double currentDistance, long currentTime) {
        if (currentTime - lastProgressCheck < PROGRESS_CHECK_INTERVAL) {
            return false;
        }

        if (lastDistance - currentDistance < MIN_PROGRESS) {
            stuckChecks++;
        } else {
            stuckChecks = 0;
        }

        lastDistance = currentDistance;
        lastProgressCheck = currentTime;

        return stuckChecks >= 3;
    }

    public boolean isDrinking() {
        return drinking;
    }

    public void startDrinking(int amount) {
        drinking = true;
        remainingDrinkAmount = amount;
    }

    public void stopDrinking() {
        drinking = false;
        remainingDrinkAmount = 0;
    }

    public int getRemainingDrinkAmount() {
        return remainingDrinkAmount;
    }

    public void reduceRemainingDrinkAmount(int amount) {
        remainingDrinkAmount = Math.max(0, remainingDrinkAmount - amount);
    }

    public long getNextDrinkDecisionTime() {
        return nextDrinkDecisionTime;
    }

    public void setNextDrinkDecisionTime(long time) {
        nextDrinkDecisionTime = time;
    }

    public long getNextDrinkPointTime() {
        return nextDrinkPointTime;
    }

    public void setNextDrinkPointTime(long time) {
        nextDrinkPointTime = time;
    }
}