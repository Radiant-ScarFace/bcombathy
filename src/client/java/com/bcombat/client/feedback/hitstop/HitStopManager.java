package com.bcombat.client.feedback.hitstop;

import com.bcombat.client.feedback.config.FeedbackConstants;

/**
 * Tracks the currently active "hit stop" (freeze-frame) window and
 * exposes the render-frozen tick-delta {@link com.bcombat.client.mixin.HitStopMixin}
 * substitutes into {@code GameRenderer#render} while active.
 * <p>
 * This is a purely visual effect: nothing here touches world time,
 * entity ticking, or any combat state — {@code CombatController} keeps
 * ticking normally underneath. Hit stop works by briefly clamping the
 * interpolation fraction the renderer uses, so entities and the camera
 * appear to freeze for a handful of frames even though the game clock
 * hasn't paused, exactly the "freeze frame" effect games like this are
 * expected to have on a heavy or decisive hit.
 * <p>
 * {@link #trigger(int)} is additive-safe but not additive: a new trigger
 * only extends the freeze if it's longer than what's already remaining,
 * so rapid repeated triggers (e.g. multiple hits the same tick) can't
 * stack into an absurdly long freeze.
 */
public final class HitStopManager {

    private static int ticksRemaining = 0;
    private static float frozenTickDelta = 0.0f;

    private HitStopManager() {
        // Static holder, no instances.
    }

    /**
     * Requests a hit-stop window of {@code ticks} client render-ticks.
     * No-ops if hit stop is disabled via config. Safe to call from any
     * client-side combat-event listener.
     */
    public static void trigger(int ticks) {
        if (!FeedbackConstants.FEEDBACK_ENABLED || !FeedbackConstants.HIT_STOP_ENABLED || ticks <= 0) {
            return;
        }
        if (ticks > ticksRemaining) {
            ticksRemaining = ticks;
        }
    }

    /**
     * Called once per rendered frame (from {@code HitStopMixin}) with the
     * tick delta the frame was about to use. Returns the tick delta the
     * frame should actually use: the real value if hit stop is inactive,
     * or a frozen (held-over) value while active.
     */
    public static float applyToTickDelta(float realTickDelta) {
        if (ticksRemaining <= 0) {
            frozenTickDelta = realTickDelta;
            return realTickDelta;
        }
        return frozenTickDelta;
    }

    /**
     * Advances the freeze-frame countdown by one. Called once per client
     * tick (not per frame) so the freeze duration is expressed in a
     * frame-rate-independent unit, matching every other duration this
     * framework configures in ticks.
     */
    public static void onClientTick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public static boolean isActive() {
        return ticksRemaining > 0;
    }

    public static int getTicksRemaining() {
        return ticksRemaining;
    }
}