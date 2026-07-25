package com.bcombat.combat.state;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Owns the current {@link CombatState} for a single player and enforces
 * the transition graph declared on {@link CombatState}. This is the only
 * class permitted to change a player's combat state.
 * <p>
 * One instance of this class exists per player (see
 * {@code com.bcombat.combat.controller.CombatController}). It has no
 * knowledge of Minecraft entities, movement, or animation — it is a pure
 * state machine, which keeps it trivially testable and reusable if this
 * mod ever needs to drive combat state for non-player entities.
 */
public final class CombatStateManager {

    private CombatState currentState;
    private BiConsumer<CombatState, CombatState> onTransition;

    public CombatStateManager() {
        this.currentState = CombatState.NORMAL;
    }

    /**
     * Registers a callback invoked with (previousState, newState) whenever
     * a transition succeeds. The controller uses this to fire
     * {@code CombatStateChangedEvent} without this class needing to know
     * about the event system.
     */
    public void setOnTransition(BiConsumer<CombatState, CombatState> onTransition) {
        this.onTransition = onTransition;
    }

    public CombatState getCurrentState() {
        return currentState;
    }

    /**
     * Attempts to move to {@code targetState}. Fails silently (returns false)
     * if the transition is not declared as legal on the current state, so
     * calling code never needs to duplicate the transition graph in
     * conditionals — it simply checks the return value if it cares.
     *
     * @return true if the transition was applied.
     */
    public boolean transitionTo(CombatState targetState) {
        Objects.requireNonNull(targetState, "targetState must not be null");

        if (targetState == currentState) {
            return false;
        }

        if (!currentState.allowedNextStates().contains(targetState)) {
            return false;
        }

        applyTransition(targetState);
        return true;
    }

    /**
     * Bypasses the transition graph entirely. This exists for exactly one
     * purpose: emergency de-escalation (e.g. the player starts swimming or
     * flying and combat mode must end immediately regardless of what state
     * they were in). This is intentionally the single documented escape
     * hatch — no other code path should skip validation.
     */
    public void forceTransitionTo(CombatState targetState) {
        Objects.requireNonNull(targetState, "targetState must not be null");
        if (targetState == currentState) {
            return;
        }
        applyTransition(targetState);
    }

    private void applyTransition(CombatState targetState) {
        CombatState previous = currentState;
        currentState = targetState;
        if (onTransition != null) {
            onTransition.accept(previous, targetState);
        }
    }
}
