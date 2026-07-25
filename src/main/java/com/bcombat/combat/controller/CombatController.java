package com.bcombat.combat.controller;

import com.bcombat.combat.animation.AnimationController;
import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.BlockController;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.events.AttackDirectionChangedEvent;
import com.bcombat.combat.events.AttackPreparationCancelledEvent;
import com.bcombat.combat.events.AttackPreparationStartedEvent;
import com.bcombat.combat.events.AttackRecoveryStartedEvent;
import com.bcombat.combat.events.AttackReleasedEvent;
import com.bcombat.combat.events.BlockEndedEvent;
import com.bcombat.combat.events.BlockStartedEvent;
import com.bcombat.combat.events.CombatEnterEvent;
import com.bcombat.combat.events.CombatEvents;
import com.bcombat.combat.events.CombatExitEvent;
import com.bcombat.combat.events.CombatStateChangedEvent;
import com.bcombat.combat.events.GuardDirectionChangedEvent;
import com.bcombat.combat.events.MovementModeChangedEvent;
import com.bcombat.combat.movement.MovementMode;
import com.bcombat.combat.movement.MovementModifierManager;
import com.bcombat.combat.player.CombatModeGuard;
import com.bcombat.combat.player.CombatStance;
import com.bcombat.combat.state.CombatState;
import com.bcombat.combat.state.CombatStateManager;
import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.player.PlayerEntity;

/**
 * The single per-player entry point for the combat framework. Every future
 * system (weapons, damage, stamina, AI, networking) should interact with a
 * player's combat state exclusively through this class rather than reaching
 * into {@link CombatStateManager}, {@link MovementModifierManager}, or
 * {@link AnimationController} directly.
 * <p>
 * One instance exists per player, owned by {@link CombatControllerManager}.
 */
public final class CombatController {

    private final PlayerEntity player;
    private final CombatStateManager stateManager = new CombatStateManager();
    private final MovementModifierManager movementModifierManager = new MovementModifierManager();
    private final AnimationController animationController = new AnimationController();
    private final BlockController blockController = new BlockController();

    private MovementMode movementMode = MovementMode.NORMAL;
    private int transitionTicksRemaining = 0;

    private AttackDirection attackDirection = AttackDirection.NONE;
    private int windUpTicksElapsed = 0;
    private boolean releaseBuffered = false;
    private boolean nextAttackBuffered = false;

    public CombatController(PlayerEntity player) {
        this.player = player;
        this.stateManager.setOnTransition(this::onStateTransition);
    }

    // ------------------------------------------------------------------
    // Public API - future systems call these
    // ------------------------------------------------------------------

    /**
     * Requests the player begin entering Combat Mode. No-op if already in
     * combat or if guard conditions (swimming/flying) prevent it.
     */
    public void requestEnterCombat() {
        if (stateManager.getCurrentState() != CombatState.NORMAL) {
            return;
        }
        if (CombatModeGuard.shouldForceExitCombat(player)) {
            return;
        }
        stateManager.transitionTo(CombatState.ENTERING_COMBAT);
    }

    /**
     * Requests the player begin exiting Combat Mode. No-op if already
     * outside combat entirely.
     */
    public void requestExitCombat() {
        CombatState current = stateManager.getCurrentState();
        if (current == CombatState.NORMAL || current == CombatState.EXITING_COMBAT) {
            return;
        }
        // Any in-combat sub-state may be interrupted directly into exiting;
        // this is intentionally permissive so releasing the combat key
        // always works regardless of what future sub-state (blocking,
        // preparing an attack, etc.) the player is currently in.
        stateManager.forceTransitionTo(CombatState.EXITING_COMBAT);
    }

    /**
     * Reserved hook for the future attack system to begin an attack
     * wind-up. Only succeeds from {@code COMBAT_IDLE}.
     */
    public void requestPrepareAttack() {
        if (stateManager.transitionTo(CombatState.PREPARING_ATTACK)) {
            windUpTicksElapsed = 0;
            releaseBuffered = false;
            attackDirection = AttackDirection.NONE;
            CombatEvents.ATTACK_PREPARATION_STARTED.invoker()
                    .onAttackPreparationStarted(new AttackPreparationStartedEvent(player));
        }
    }

    /**
     * Reserved hook for the future attack system to cancel an attack
     * wind-up. Only succeeds from {@code PREPARING_ATTACK}.
     */
    public void cancelPrepareAttack() {
        if (stateManager.getCurrentState() != CombatState.PREPARING_ATTACK) {
            return;
        }
        if (stateManager.transitionTo(CombatState.COMBAT_IDLE)) {
            CombatEvents.ATTACK_PREPARATION_CANCELLED.invoker()
                    .onAttackPreparationCancelled(new AttackPreparationCancelledEvent(player));
        }
    }

    /**
     * Updates the direction the current wind-up is committed to. No-op
     * outside {@code PREPARING_ATTACK}. Fires {@link
     * AttackDirectionChangedEvent} only when the direction actually
     * changes, so listeners never see redundant events.
     */
    public void updateAttackDirection(AttackDirection newDirection) {
        if (stateManager.getCurrentState() != CombatState.PREPARING_ATTACK) {
            return;
        }
        if (newDirection == attackDirection) {
            return;
        }
        AttackDirection previous = attackDirection;
        attackDirection = newDirection;
        CombatEvents.ATTACK_DIRECTION_CHANGED.invoker()
                .onAttackDirectionChanged(new AttackDirectionChangedEvent(player, previous, newDirection));
    }

    /**
     * Requests the wind-up commit into the attack. Only meaningful from
     * {@code PREPARING_ATTACK}; if called before {@link
     * CombatConstants#MIN_ATTACK_PREPARATION_TICKS} has elapsed, the
     * request is buffered and applied automatically the instant that
     * minimum is reached, rather than dropped.
     */
    public void releaseAttack() {
        if (stateManager.getCurrentState() != CombatState.PREPARING_ATTACK) {
            return;
        }
        releaseBuffered = true;
    }

    /**
     * Reserved hook for input to remember an attack request made while
     * still in {@code RECOVERY}, so it isn't silently dropped. This does
     * NOT start the next attack early — the buffered request is only ever
     * honored the instant {@code RECOVERY} naturally completes, never
     * sooner, so "prevent attack spam" and "don't drop a legitimate
     * follow-up input" both hold at once. No-op outside {@code RECOVERY}.
     */
    public void bufferNextAttack() {
        if (stateManager.getCurrentState() != CombatState.RECOVERY) {
            return;
        }
        nextAttackBuffered = true;
    }

    /**
     * Requests the player begin entering a block. Only succeeds from
     * {@code COMBAT_IDLE} — attempting to block while winding up or
     * recovering from an attack is a no-op, exactly like the reverse
     * (attempting to attack while blocking) is a no-op in {@link
     * #requestPrepareAttack()}. Since only one {@code CombatState} is
     * ever active at a time, this mutual exclusion falls directly out of
     * the state machine and needs no extra bookkeeping here.
     */
    public void requestEnterBlock() {
        stateManager.transitionTo(CombatState.ENTER_BLOCK);
    }

    /**
     * Requests the player begin exiting a block. No-op unless currently
     * in {@code ENTER_BLOCK} or {@code BLOCK_IDLE}. Uses {@code
     * forceTransitionTo} the same way {@link #requestExitCombat()} does,
     * so releasing the block key always works even mid-way through the
     * enter transition.
     */
    public void requestExitBlock() {
        CombatState current = stateManager.getCurrentState();
        if (current != CombatState.ENTER_BLOCK && current != CombatState.BLOCK_IDLE) {
            return;
        }
        stateManager.forceTransitionTo(CombatState.EXIT_BLOCK);
    }

    /**
     * Proposes a new guard direction, as resolved from mouse movement by
     * the client-side guard direction tracker. No-op outside {@code
     * ENTER_BLOCK}/{@code BLOCK_IDLE}. Delegates the accept/reject
     * decision to {@link BlockController#requestDirection}, and fires
     * {@link GuardDirectionChangedEvent} only when the proposal is
     * actually accepted, so listeners never see redundant events.
     */
    public void updateGuardDirection(GuardDirection proposed) {
        CombatState current = stateManager.getCurrentState();
        if (current != CombatState.ENTER_BLOCK && current != CombatState.BLOCK_IDLE) {
            return;
        }

        GuardDirection previous = blockController.getCurrentDirection();
        if (blockController.requestDirection(proposed)) {
            CombatEvents.GUARD_DIRECTION_CHANGED.invoker()
                    .onGuardDirectionChanged(new GuardDirectionChangedEvent(player, previous, blockController.getCurrentDirection()));
        }
    }

    public CombatState getCombatState() {
        return stateManager.getCurrentState();
    }

    public MovementMode getMovementMode() {
        return movementMode;
    }

    public CombatStance getCombatStance() {
        return stateManager.getCurrentState().isCombatActive()
                ? CombatStance.genericCombat()
                : CombatStance.none();
    }

    public AnimationController getAnimationController() {
        return animationController;
    }

    public AttackDirection getAttackDirection() {
        return attackDirection;
    }

    public GuardDirection getGuardDirection() {
        return blockController.getCurrentDirection();
    }

    // ------------------------------------------------------------------
    // Per-tick driver - called by CombatControllerManager
    // ------------------------------------------------------------------

    /**
     * Advances this player's combat state, movement modifiers, and
     * animation controller by one tick. Must be called every tick this
     * controller is active.
     */
    public void tick() {
        applyGuardConditions();
        advanceTransitionTimers();
        movementModifierManager.tick(player);
        blockController.tick();
        animationController.tick(player, stateManager.getCurrentState(), movementMode, attackDirection, blockController.getCurrentDirection());
    }

    private void applyGuardConditions() {
        if (stateManager.getCurrentState() != CombatState.NORMAL
                && stateManager.getCurrentState() != CombatState.EXITING_COMBAT
                && CombatModeGuard.shouldForceExitCombat(player)) {
            forceImmediateExit();
        }
    }

    /**
     * Skips the exit transition entirely and returns straight to NORMAL.
     * Used only for guard conditions (swim/fly), where the exit must be
     * instantaneous rather than eased.
     */
    private void forceImmediateExit() {
        stateManager.forceTransitionTo(CombatState.EXITING_COMBAT);
        stateManager.forceTransitionTo(CombatState.NORMAL);
    }

    private void advanceTransitionTimers() {
        CombatState current = stateManager.getCurrentState();

        if (current == CombatState.ENTERING_COMBAT) {
            if (transitionTicksRemaining <= 0) {
                stateManager.transitionTo(CombatState.COMBAT_IDLE);
            } else {
                transitionTicksRemaining--;
            }
        } else if (current == CombatState.EXITING_COMBAT) {
            if (transitionTicksRemaining <= 0) {
                stateManager.transitionTo(CombatState.NORMAL);
            } else {
                transitionTicksRemaining--;
            }
        } else if (current == CombatState.PREPARING_ATTACK) {
            windUpTicksElapsed++;
            if (releaseBuffered && windUpTicksElapsed >= CombatConstants.MIN_ATTACK_PREPARATION_TICKS) {
                beginAttackRelease();
            }
        } else if (current == CombatState.ATTACKING) {
            if (transitionTicksRemaining <= 0) {
                stateManager.transitionTo(CombatState.RECOVERY);
            } else {
                transitionTicksRemaining--;
            }
        } else if (current == CombatState.RECOVERY) {
            if (transitionTicksRemaining <= 0) {
                boolean chainIntoAttack = nextAttackBuffered;
                nextAttackBuffered = false;
                stateManager.transitionTo(CombatState.COMBAT_IDLE);
                if (chainIntoAttack) {
                    requestPrepareAttack();
                }
            } else {
                transitionTicksRemaining--;
            }
        } else if (current == CombatState.ENTER_BLOCK) {
            if (transitionTicksRemaining <= 0) {
                stateManager.transitionTo(CombatState.BLOCK_IDLE);
            } else {
                transitionTicksRemaining--;
            }
        } else if (current == CombatState.EXIT_BLOCK) {
            if (transitionTicksRemaining <= 0) {
                stateManager.transitionTo(CombatState.COMBAT_IDLE);
            } else {
                transitionTicksRemaining--;
            }
        }
    }

    /**
     * Commits a buffered release request into the actual
     * {@code PREPARING_ATTACK -> ATTACKING} transition once the minimum
     * wind-up duration has elapsed.
     */
    private void beginAttackRelease() {
        if (stateManager.transitionTo(CombatState.ATTACKING)) {
            transitionTicksRemaining = CombatConstants.ATTACK_RELEASE_DURATION_TICKS;
            releaseBuffered = false;
            CombatEvents.ATTACK_RELEASED.invoker()
                    .onAttackReleased(new AttackReleasedEvent(player, attackDirection));
        }
    }

    // ------------------------------------------------------------------
    // Internal transition side-effects
    // ------------------------------------------------------------------

    private void onStateTransition(CombatState previous, CombatState current) {
        CombatEvents.COMBAT_STATE_CHANGED.invoker()
                .onCombatStateChanged(new CombatStateChangedEvent(player, previous, current));

        if (previous == CombatState.NORMAL && current == CombatState.ENTERING_COMBAT) {
            transitionTicksRemaining = CombatConstants.ENTER_COMBAT_TRANSITION_TICKS;
            setMovementMode(MovementMode.COMBAT);
            movementModifierManager.enableCombatMovement(player);
            CombatEvents.COMBAT_ENTER.invoker().onCombatEnter(new CombatEnterEvent(player));
        }

        if (current == CombatState.EXITING_COMBAT) {
            transitionTicksRemaining = CombatConstants.EXIT_COMBAT_TRANSITION_TICKS;
            CombatEvents.COMBAT_EXIT.invoker().onCombatExit(new CombatExitEvent(player));
        }

        if (current == CombatState.NORMAL) {
            setMovementMode(MovementMode.NORMAL);
            movementModifierManager.disableCombatMovement(player);
            nextAttackBuffered = false;
            blockController.reset();
        }

        if (current == CombatState.PREPARING_ATTACK) {
            movementModifierManager.enableWindUpPenalty(player);
        } else if (previous == CombatState.PREPARING_ATTACK) {
            // Covers every way a wind-up can end: release into ATTACKING,
            // a cancelled wind-up back to COMBAT_IDLE, or a forced exit.
            // disableCombatMovement() above already strips this when the
            // path leads all the way to NORMAL, but that call only fires
            // for that specific branch, so this is the single place that
            // guarantees the penalty never outlives PREPARING_ATTACK.
            movementModifierManager.disableWindUpPenalty(player);
        }

        if (current == CombatState.RECOVERY) {
            transitionTicksRemaining = CombatConstants.RECOVERY_DURATION_TICKS;
            CombatEvents.ATTACK_RECOVERY_STARTED.invoker()
                    .onAttackRecoveryStarted(new AttackRecoveryStartedEvent(player));
        }

        if (current == CombatState.ENTER_BLOCK) {
            transitionTicksRemaining = CombatConstants.ENTER_BLOCK_TRANSITION_TICKS;
            blockController.reset();
            CombatEvents.BLOCK_STARTED.invoker().onBlockStarted(new BlockStartedEvent(player));
        }

        if (current == CombatState.EXIT_BLOCK) {
            transitionTicksRemaining = CombatConstants.EXIT_BLOCK_TRANSITION_TICKS;
            CombatEvents.BLOCK_ENDED.invoker().onBlockEnded(new BlockEndedEvent(player));
        }

        if (current == CombatState.COMBAT_IDLE) {
            // Covers arrival from ENTERING_COMBAT, RECOVERY, a cancelled
            // wind-up, or a resolved feint - direction never carries over
            // into the next wind-up.
            attackDirection = AttackDirection.NONE;
            // Covers arrival from a completed EXIT_BLOCK the same way -
            // guard direction never carries over into the next block.
            blockController.reset();
        }
    }

    private void setMovementMode(MovementMode newMode) {
        if (newMode == movementMode) {
            return;
        }
        MovementMode previous = movementMode;
        movementMode = newMode;
        CombatEvents.MOVEMENT_MODE_CHANGED.invoker()
                .onMovementModeChanged(new MovementModeChangedEvent(player, previous, newMode));
    }
}