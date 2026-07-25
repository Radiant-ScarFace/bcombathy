package com.bcombat.combat.block;

import com.bcombat.combat.util.CombatConstants;

/**
 * The dedicated per-player controller for the defensive/blocking system.
 * Owned by {@code CombatController} exactly the same way it owns {@code
 * MovementModifierManager} and {@code AnimationController} — this class
 * has no knowledge of {@code CombatStateManager} or Minecraft entities,
 * only of guard direction, which keeps it trivially testable.
 * <p>
 * Responsibilities in this phase:
 * <ul>
 *     <li>Own the currently locked {@link GuardDirection}.</li>
 *     <li>Decide whether a newly proposed direction should be accepted,
 *     applying a temporal debounce ({@link CombatConstants#GUARD_SWITCH_DELAY_TICKS})
 *     so brief mouse jitter near a directional boundary can't rapidly
 *     flip the guard back and forth.</li>
 * </ul>
 * <p>
 * Spatial classification — which direction a given mouse movement maps
 * to, and the deadzone that keeps small movements from registering at
 * all — is deliberately handled upstream by the client-side {@code
 * GuardDirectionTracker}, not here. This class only ever receives an
 * already-classified {@link GuardDirection} and decides if/when to lock
 * onto it, the same separation of concerns used for attacks ({@code
 * AttackDirectionTracker} classifies, {@code CombatController} owns the
 * committed value).
 * <p>
 * Reserved extension points (intentionally not stubbed out, since they
 * have no calling code yet): a future perfect-block phase would check
 * whether the locked direction matched an incoming hit within {@link
 * CombatConstants#PERFECT_BLOCK_WINDOW_TICKS_RESERVED} of impact; a
 * future chamber-block phase would drive {@code CombatState.CHAMBER}
 * from here; a future stamina phase would gate {@link #requestDirection}
 * and block entry on available stamina. All three read the same locked
 * direction this class already exposes.
 */
public final class BlockController {

    private GuardDirection currentDirection = GuardDirection.NONE;
    private int ticksSinceDirectionChange = CombatConstants.GUARD_SWITCH_DELAY_TICKS;

    /**
     * Clears any locked direction. Called by {@code CombatController} the
     * instant {@code CombatState.ENTER_BLOCK} begins, so a stale direction
     * from a previous block is never reused.
     */
    public void reset() {
        currentDirection = GuardDirection.NONE;
        ticksSinceDirectionChange = CombatConstants.GUARD_SWITCH_DELAY_TICKS;
    }

    /**
     * Advances the switch-delay timer. Must be called once per tick while
     * this controller is relevant (i.e. {@code ENTER_BLOCK} or {@code
     * BLOCK_IDLE}).
     */
    public void tick() {
        if (ticksSinceDirectionChange < CombatConstants.GUARD_SWITCH_DELAY_TICKS) {
            ticksSinceDirectionChange++;
        }
    }

    /**
     * Proposes a new guard direction, as resolved from mouse movement.
     * The proposal is accepted, and {@link #getCurrentDirection()} updated,
     * only if it differs from the currently locked direction and the
     * switch-delay debounce has elapsed since the last accepted change.
     * <p>
     * {@link GuardDirection#NONE} is never accepted as a change once a
     * real direction is locked — the guard direction remains locked while
     * blocking until the player deliberately moves into a different
     * direction's zone, exactly as the design calls for. The very first
     * direction lock (from {@code NONE}) is never delayed by the debounce,
     * only subsequent switches away from an already-locked direction are.
     *
     * @return true if the proposal was accepted and the direction changed.
     */
    public boolean requestDirection(GuardDirection proposed) {
        if (proposed == GuardDirection.NONE || proposed == currentDirection) {
            return false;
        }
        if (ticksSinceDirectionChange < CombatConstants.GUARD_SWITCH_DELAY_TICKS) {
            return false;
        }

        currentDirection = proposed;
        ticksSinceDirectionChange = 0;
        return true;
    }

    public GuardDirection getCurrentDirection() {
        return currentDirection;
    }
}