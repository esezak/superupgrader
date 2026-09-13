package com.esezak.superupgrader.logic;

import com.esezak.superupgrader.config.ModConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player cooldown after each wager.
 * After a spin, the player must wait attemptCooldownSeconds before spinning again.
 * The cooldown resets after each wager (not a rolling window).
 */
public class CooldownTracker {
    // Maps player UUID -> timestamp (millis) of their last spin
    private static final Map<UUID, Long> LAST_SPIN = new ConcurrentHashMap<>();

    /**
     * Checks if the player can spin right now.
     *
     * @return true if the player is allowed to spin
     */
    public static boolean canSpin(UUID playerId) {
        return getRemainingCooldownMs(playerId) == 0;
    }

    /**
     * Returns remaining cooldown in milliseconds, or 0 if ready.
     */
    public static long getRemainingCooldownMs(UUID playerId) {
        int cooldownSeconds = ModConfig.get().attemptCooldownSeconds;

        Long lastSpin = LAST_SPIN.get(playerId);
        if (lastSpin == null) return 0;

        long elapsed = System.currentTimeMillis() - lastSpin;
        long cooldownMs = Math.max(SpinResult.DURATION_MS, cooldownSeconds * 1000L);
        return Math.max(0, cooldownMs - elapsed);
    }

    /**
     * Records that the player just performed a spin.
     */
    public static void recordSpin(UUID playerId) {
        LAST_SPIN.put(playerId, System.currentTimeMillis());
    }

    /**
     * Clears the cooldown for a player (e.g. on disconnect).
     */
    public static void clear(UUID playerId) {
        LAST_SPIN.remove(playerId);
    }
}
