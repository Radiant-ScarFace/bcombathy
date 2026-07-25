package com.bcombat.combat.animation;

import com.bcombat.combat.util.CombatConstants;
import com.bcombat.combat.util.MathUtil;

/**
 * Tracks a smooth 0-1 blend weight between a previous and current
 * {@link AnimationState} over {@link CombatConstants#ANIMATION_BLEND_DURATION_TICKS}.
 * This is generic and reusable: it knows nothing about which states are
 * "combat" states, only that a transition from A to B should ease in
 * rather than snap. A future GeckoLib model layer reads
 * {@link #getBlendWeight()} to crossfade animation tracks.
 */
public final class AnimationBlender {

    private AnimationState previousState;
    private AnimationState currentState;
    private int ticksSinceChange;
    private int activeBlendDurationTicks;

    public AnimationBlender(AnimationState initialState) {
        this.previousState = initialState;
        this.currentState = initialState;
        this.activeBlendDurationTicks = CombatConstants.ANIMATION_BLEND_DURATION_TICKS;
        this.ticksSinceChange = activeBlendDurationTicks;
    }

    /**
     * Begins a new blend if the target state differs from the current one,
     * using the default {@link CombatConstants#ANIMATION_BLEND_DURATION_TICKS}
     * blend speed. Safe to call every tick with the same value; it is a
     * no-op once already targeting that state.
     */
    public void setTargetState(AnimationState targetState) {
        setTargetState(targetState, CombatConstants.ANIMATION_BLEND_DURATION_TICKS);
    }

    /**
     * Begins a new blend using a caller-specified blend duration, so
     * different transitions can blend at different speeds (e.g. a
     * snappier blend into a Perfect Block/Parry/Chamber reaction than
     * ordinary locomotion). Safe to call every tick with the same value;
     * it is a no-op once already targeting that state — the blend speed
     * a transition started with is what plays out even if a later call
     * this same tick passes a different duration for a state that hasn't
     * actually changed.
     */
    public void setTargetState(AnimationState targetState, int blendDurationTicks) {
        if (targetState == currentState) {
            return;
        }
        this.previousState = currentState;
        this.currentState = targetState;
        this.ticksSinceChange = 0;
        this.activeBlendDurationTicks = Math.max(1, blendDurationTicks);
    }

    /**
     * Advances the blend timer. Must be called once per tick.
     */
    public void tick() {
        if (ticksSinceChange < activeBlendDurationTicks) {
            ticksSinceChange++;
        }
    }

    public AnimationState getPreviousState() {
        return previousState;
    }

    public AnimationState getCurrentState() {
        return currentState;
    }

    /**
     * @return smoothed progress from previousState (0.0) to currentState (1.0).
     */
    public float getBlendWeight() {
        return MathUtil.tickProgress(ticksSinceChange, activeBlendDurationTicks);
    }
}