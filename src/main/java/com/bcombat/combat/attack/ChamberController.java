package com.bcombat.combat.attack;

import com.bcombat.combat.defense.IncomingAttack;
import net.minecraft.entity.player.PlayerEntity;

/**
 * The dedicated per-player controller for a single in-progress chamber
 * attempt. Owned by {@code CombatController} the same way it owns
 * {@code BlockController} and {@code AnimationController} — this class
 * has no knowledge of {@code CombatStateManager} or timing windows,
 * only of the pending attempt's data, which keeps it trivially testable.
 * <p>
 * A chamber attempt is a two-step process spanning {@code
 * CombatState.CHAMBER_PREPARE} (this class records the pending outcome)
 * and its resolution a few ticks later into either {@code
 * CHAMBER_SUCCESS} or back into {@code PREPARING_ATTACK}. Direction and
 * timing matching against the incoming attack is decided by {@code
 * CombatController} — this class only remembers the result until it's
 * needed.
 */
public final class ChamberController {

    private PlayerEntity pendingAttacker;
    private AttackDirection pendingDirection = AttackDirection.NONE;
    private boolean pendingTimingSuccess;
    private boolean active;

    /**
     * Records a chamber attempt that has just begun. {@code
     * timingSuccess} is decided up-front by {@code CombatController} from
     * the incoming attack's timing, since the attempt is evaluated once
     * at the moment of notification, not re-evaluated every tick while
     * {@code CHAMBER_PREPARE} plays out.
     */
    public void begin(IncomingAttack incoming, boolean timingSuccess) {
        this.pendingAttacker = incoming.attacker();
        this.pendingDirection = incoming.direction();
        this.pendingTimingSuccess = timingSuccess;
        this.active = true;
    }

    /**
     * Clears the pending attempt. Called by {@code CombatController} the
     * instant {@code CHAMBER_PREPARE} resolves (either outcome), so a
     * stale attempt is never reused.
     */
    public void reset() {
        pendingAttacker = null;
        pendingDirection = AttackDirection.NONE;
        pendingTimingSuccess = false;
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public boolean wasTimingSuccessful() {
        return pendingTimingSuccess;
    }

    public PlayerEntity getPendingAttacker() {
        return pendingAttacker;
    }

    public AttackDirection getPendingDirection() {
        return pendingDirection;
    }
}