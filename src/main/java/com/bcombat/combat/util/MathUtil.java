package com.bcombat.combat.util;

/**
 * Small, dependency-free math helpers used across the combat framework.
 * Kept separate from Minecraft's {@code MathHelper} so this framework's
 * math utilities are explicit about what they're used for (blending,
 * transition progress) rather than general-purpose.
 */
public final class MathUtil {

    private MathUtil() {
        // Utility class, no instances.
    }

    /**
     * Clamps a value between a minimum and maximum, inclusive.
     */
    public static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    /**
     * Linearly interpolates between {@code start} and {@code end} by {@code progress},
     * where progress is expected to be in the range [0, 1].
     */
    public static float lerp(float start, float end, float progress) {
        return start + (end - start) * clamp(progress, 0.0f, 1.0f);
    }

    /**
     * Smoothstep easing (3t^2 - 2t^3) for a progress value in [0, 1].
     * Produces a smooth ease-in/ease-out curve, avoiding the "snapping"
     * that linear blending can produce at the start/end of a transition.
     */
    public static float smoothstep(float progress) {
        float t = clamp(progress, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    /**
     * Converts a completed-tick-count and total-duration into a smoothed [0, 1] progress value.
     */
    public static float tickProgress(int ticksElapsed, int totalTicks) {
        if (totalTicks <= 0) {
            return 1.0f;
        }
        return smoothstep((float) ticksElapsed / (float) totalTicks);
    }
}
