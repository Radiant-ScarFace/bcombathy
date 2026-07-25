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

    public AnimationBlender(AnimationState initialState) {
        this.previousState = initialState;
        this.currentState = initialState;
        this.ticksSinceChange = CombatConstants.ANIMATION_BLEND_DURATION_TICKS;
    }

    /**
     * Begins a new blend if the target state differs from the current one.
     * Safe to call every tick with the same value; it is a no-op once
     * already targeting that state.
     */
    public void setTargetState(AnimationState targetState) {
        if (targetState == currentState) {
            return;
        }
        this.previousState = currentState;
        this.currentState = targetState;
        this.ticksSinceChange = 0;
    }

    /**
     * Advances the blend timer. Must be called once per tick.
     */
    public void tick() {
        if (ticksSinceChange < CombatConstants.ANIMATION_BLEND_DURATION_TICKS) {
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
        return MathUtil.tickProgress(ticksSinceChange, CombatConstants.ANIMATION_BLEND_DURATION_TICKS);
    }
}
