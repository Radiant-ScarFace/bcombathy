package com.bcombat.client.feedback.camera;

import com.bcombat.client.feedback.config.FeedbackConstants;

import java.util.Random;

/**
 * Trauma-based camera shake, in the style commonly used for game-feel
 * screen shake: an internal "trauma" value in {@code [0, 1]} is added to
 * on impact events and decays linearly every tick; the actual yaw/pitch
 * offset applied to the camera each frame is trauma squared (so shake
 * falls off quickly at the tail rather than dragging out at low
 * intensity) times the configured max angle, modulated by simplex-free
 * pseudo-noise so the shake reads as organic jitter rather than a clean
 * sine wave.
 * <p>
 * Consumed by {@code com.bcombat.client.mixin.CameraShakeMixin}, which
 * nudges {@code Camera}'s rotation after every vanilla update. Entirely
 * visual: never touches the player entity's actual look direction, only
 * the render camera, so it has zero effect on aim, hit detection, or any
 * other combat mechanic.
 */
public final class CameraShakeManager {

    private static double trauma = 0.0;
    private static final Random NOISE = new Random();
    private static double noiseSeedYaw = NOISE.nextDouble() * 1000.0;
    private static double noiseSeedPitch = NOISE.nextDouble() * 1000.0;
    private static double timeAccumulator = 0.0;

    private CameraShakeManager() {
        // Static holder, no instances.
    }

    /**
     * Adds {@code amount} (expected {@code [0, 1]}) of trauma, clamped
     * to a maximum of 1.0. No-ops if camera shake is disabled via
     * config.
     */
    public static void addTrauma(double amount) {
        if (!FeedbackConstants.FEEDBACK_ENABLED || !FeedbackConstants.CAMERA_SHAKE_ENABLED || amount <= 0.0) {
            return;
        }
        trauma = Math.min(1.0, trauma + amount);
    }

    /** Advances decay by one tick. Called once per client tick. */
    public static void onClientTick() {
        if (trauma > 0.0) {
            trauma = Math.max(0.0, trauma - FeedbackConstants.CAMERA_SHAKE_DECAY_PER_TICK);
        }
        timeAccumulator += 1.0;
    }

    /** @return the current yaw offset in degrees to apply this frame. */
    public static float getYawOffsetDegrees(float partialTicks) {
        if (trauma <= 0.0) {
            return 0.0f;
        }
        double shake = trauma * trauma;
        double t = (timeAccumulator + partialTicks) * FeedbackConstants.CAMERA_SHAKE_FREQUENCY;
        double noise = pseudoNoise(noiseSeedYaw + t);
        return (float) (noise * shake * FeedbackConstants.CAMERA_SHAKE_MAX_ANGLE_DEGREES);
    }

    /** @return the current pitch offset in degrees to apply this frame. */
    public static float getPitchOffsetDegrees(float partialTicks) {
        if (trauma <= 0.0) {
            return 0.0f;
        }
        double shake = trauma * trauma;
        double t = (timeAccumulator + partialTicks) * FeedbackConstants.CAMERA_SHAKE_FREQUENCY;
        double noise = pseudoNoise(noiseSeedPitch + t);
        return (float) (noise * shake * FeedbackConstants.CAMERA_SHAKE_MAX_ANGLE_DEGREES * 0.6);
    }

    public static boolean isActive() {
        return trauma > 0.0;
    }

    /**
     * Cheap deterministic pseudo-noise in {@code [-1, 1]}, built from
     * layered sine waves rather than a true noise function - sufficient
     * for a subtle screen-shake jitter without pulling in a dependency.
     */
    private static double pseudoNoise(double t) {
        double a = Math.sin(t * 1.0);
        double b = Math.sin(t * 2.13 + 1.7);
        double c = Math.sin(t * 4.31 + 3.1);
        return (a * 0.5 + b * 0.3 + c * 0.2);
    }
}