package com.innspectre.ecology.water;

public class DrinkingCalculator {

    private static final int MAX_DRINK_AMOUNT = 40;
    private static final int MIN_DRINK_AMOUNT = 5;
    private static final int TICKS_PER_DRINK_POINT = 20;

    public static int calculateDrinkAmount(int thirst) {
        thirst = Math.max(0, Math.min(100, thirst));

        int missingThirst = 100 - thirst;

        if (missingThirst == 0) {
            return 0;
        }

        double thirstFactor = missingThirst / 100.0;

        double desiredAmount =
                MAX_DRINK_AMOUNT * thirstFactor * thirstFactor;

        int amount = Math.max(
                MIN_DRINK_AMOUNT,
                (int) Math.round(desiredAmount)
        );

        return Math.min(amount, missingThirst);
    }

    public static int getTicksPerDrinkPoint() {
        return TICKS_PER_DRINK_POINT;
    }
}