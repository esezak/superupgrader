package com.esezak.superupgrader.logic;

/**
 * Computes the success chance for an upgrade attempt.
 */
public class UpgradeCalculator {

    public static final double HOUSE_EDGE = 0.9;
    private static final double MIN_CHANCE = 0.001; // 0.1%
    private static final double MAX_CHANCE = 0.90;  // 90%

    /**
     * Computes the probability of a successful upgrade.
     *
     * @param inputValue  total value of the wagered items (base × durability × quantity)
     * @param targetValue value of one unit of the target item
     * @return success probability in [0.001, 0.90]
     */
    public static double successChance(double inputValue, double targetValue) {
        if (!Double.isFinite(inputValue) || !Double.isFinite(targetValue) || targetValue <= 0 || inputValue <= 0) return 0;

        double raw = HOUSE_EDGE * (inputValue / targetValue);
        return Math.max(MIN_CHANCE, Math.min(MAX_CHANCE, raw));
    }

    /**
     * Returns the chance as a display percentage string (e.g. "9.00%").
     */
    public static String formatChance(double chance) {
        return String.format("%.2f%%", chance * 100.0);
    }
}
