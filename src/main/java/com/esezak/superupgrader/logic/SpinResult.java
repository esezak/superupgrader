package com.esezak.superupgrader.logic;

import net.minecraft.network.FriendlyByteBuf;

/**
 * The result of a spin, computed server-side and sent to the client for animation.
 */
public record SpinResult(
        boolean success,
        double chance,
        double markerFinalAngle,
        double goldZoneStartAngle,
        double goldZoneArcDegrees
) {
    public static final long DURATION_MS = 3500;
    public static final double GOLD_ZONE_START_DEGREES = 0;

    /**
     * Creates a SpinResult by rolling the outcome on the server.
     *
     * @param chance success probability in [0, 1]
     * @param random a random source
     * @return a new SpinResult
     */
    public static SpinResult roll(double chance, java.util.Random random) {
        // The gold zone arc is proportional to the chance
        double arcDegrees = chance * 360.0;

        // Match the preview: zero degrees is twelve o'clock, increasing clockwise.
        double goldStart = GOLD_ZONE_START_DEGREES;

        // Determine the final angle of the marker
        boolean success = random.nextDouble() < chance;

        double markerAngle;
        if (success) {
            // Marker lands inside the gold zone
            double offset = random.nextDouble() * arcDegrees;
            markerAngle = (goldStart + offset) % 360.0;
        } else {
            // Marker lands outside the gold zone
            double failArc = 360.0 - arcDegrees;
            if (failArc <= 0) {
                // Edge case: 100% chance (shouldn't happen due to 90% cap, but be safe)
                markerAngle = goldStart;
                success = true;
            } else {
                double offset = random.nextDouble() * failArc;
                markerAngle = (goldStart + arcDegrees + offset) % 360.0;
            }
        }

        return new SpinResult(success, chance, markerAngle, goldStart, arcDegrees);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeDouble(chance);
        buf.writeDouble(markerFinalAngle);
        buf.writeDouble(goldZoneStartAngle);
        buf.writeDouble(goldZoneArcDegrees);
    }

    public static SpinResult read(FriendlyByteBuf buf) {
        boolean success = buf.readBoolean();
        double chance = buf.readDouble();
        double markerFinalAngle = buf.readDouble();
        double goldZoneStartAngle = buf.readDouble();
        double goldZoneArcDegrees = buf.readDouble();
        return new SpinResult(success, chance, markerFinalAngle, goldZoneStartAngle, goldZoneArcDegrees);
    }
}
