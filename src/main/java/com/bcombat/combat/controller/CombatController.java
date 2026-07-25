package com.bcombat.combat.controller;

import com.bcombat.combat.animation.AnimationController;
import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.attack.ChamberController;
import com.bcombat.combat.block.BlockController;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.defense.DefenseResult;
import com.bcombat.combat.defense.DirectionCompatibility;
import com.bcombat.combat.defense.IncomingAttack;
import com.bcombat.combat.events.AttackDirectionChangedEvent;
import com.bcombat.combat.events.AttackPreparationCancelledEvent;
import com.bcombat.combat.events.AttackPreparationStartedEvent;
import com.bcombat.combat.events.AttackRecoveryStartedEvent;
import com.bcombat.combat.events.AttackReleasedEvent;
import com.bcombat.combat.events.BlockEndedEvent;
import com.bcombat.combat.events.BlockStartedEvent;
import com.bcombat.combat.events.ChamberStartedEvent;
import com.bcombat.combat.events.ChamberSucceededEvent;
import com.bcombat.combat.events.CombatEnterEvent;
import com.bcombat.combat.events.CombatEvents;
import com.bcombat.combat.events.CombatExitEvent;
import com.bcombat.combat.events.CombatStateChangedEvent;
import com.bcombat.combat.events.GuardDirectionChangedEvent;
import com.bcombat.combat.events.MovementModeChangedEvent;
import com.bcombat.combat.events.ParryEvent;
import com.bcombat.combat.events.PerfectBlockEvent;
import com.bcombat.combat.events.WeaponChangedEvent;
import com.bcombat.combat.events.WeaponEquippedEvent;
import com.bcombat.combat.events.WeaponUnequippedEvent;
import com.bcombat.combat.movement.MovementMode;
import com.bcombat.combat.movement.MovementModifierManager;
import com.bcombat.combat.player.CombatModeGuard;
import com.bcombat.combat.player.CombatStance;
import com.bcombat.combat.state.CombatState;
import com.bcombat.combat.state.CombatStateManager;
import com.bcombat.combat.util.CombatConstants;
import com.bcombat.combat.weapon.WeaponController;
import com.bcombat.combat.weapon.WeaponProperties;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;

import java.util.UUID;
import java.util.Objects;

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
    private final ChamberController chamberController = new ChamberController();
    private final WeaponController weaponController = new WeaponController();

    private MovementMode movementMode = MovementMode.NORMAL;
    private int transitionTicksRemaining = 0;

    private AttackDirection attackDirection = AttackDirection.NONE;
    private int windUpTicksElapsed = 0;
    private boolean releaseBuffered = false;
    private boolean nextAttackBuffered = false;

    /**
     * Identity of the last {@link IncomingAttack} this controller resolved
     * into a Perfect Block, Parry, or Chamber. Prevents the same physical
     * swing — which a future hit-detection system may re-notify more than
     * once as it closes in — from ever double-triggering a defensive
     * mechanic. See {@link #notifyIncomingAttack}.
     */
    private UUID lastHandledAttackId;

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
        // A weapon that doesn't support this direction simply ignores the
        // proposal (the previously committed direction, if any, is kept)
        // rather than silently falling back to a different direction the
        // player didn't ask for.
        if (newDirection != AttackDirection.NONE && !weaponController.getCurrentWeapon().supportsAttackDirection(newDirection)) {
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
        if (current != CombatState.ENTER_BLOCK && current != CombatState.BLOCK_IDLE
                && current != CombatState.PERFECT_BLOCK) {
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

        // Same "ignore, don't substitute" handling as updateAttackDirection:
        // a guard position the current weapon doesn't support is simply
        // never proposed to BlockController.
        if (proposed != GuardDirection.NONE && !weaponController.getCurrentWeapon().supportsGuardDirection(proposed)) {
            return;
        }

        GuardDirection previous = blockController.getCurrentDirection();
        if (blockController.requestDirection(proposed)) {
            CombatEvents.GUARD_DIRECTION_CHANGED.invoker()
                    .onGuardDirectionChanged(new GuardDirectionChangedEvent(player, previous, blockController.getCurrentDirection()));
        }
    }

    /**
     * The extension point a future hit-detection/AI/networking system
     * calls the instant an attack is about to connect with this player,
     * so this controller can evaluate whether the defender's current
     * guard or wind-up qualifies for a Perfect Block, Parry, or Chamber.
     * <p>
     * This method resolves timing/direction and drives the state machine
     * and events only — no damage, hit registration, or stamina cost is
     * applied here, since those systems are explicitly out of scope for
     * this phase.
     * <ul>
     *     <li>From {@code ENTER_BLOCK}/{@code BLOCK_IDLE}: checked against
     *     the locked guard direction for a Perfect Block/Parry.</li>
     *     <li>From {@code PREPARING_ATTACK}: checked against the
     *     committed attack direction for a Chamber.</li>
     *     <li>From any other state: no-op, returns {@link DefenseResult#NONE}.</li>
     * </ul>
     * Safe to call more than once for the same physical swing — see
     * {@link IncomingAttack#id()} — a given identity is only ever
     * resolved once, which is what satisfies "prevent duplicate
     * triggering" for Perfect Block.
     *
     * @return which defensive mechanic (if any) resolved from this notification.
     */
    public DefenseResult notifyIncomingAttack(IncomingAttack incoming) {
        Objects.requireNonNull(incoming, "incoming must not be null");

        if (incoming.id().equals(lastHandledAttackId)) {
            return DefenseResult.NONE;
        }

        CombatState current = stateManager.getCurrentState();
        DefenseResult result = DefenseResult.NONE;

        if (current == CombatState.ENTER_BLOCK || current == CombatState.BLOCK_IDLE) {
            result = attemptBlockDefense(incoming);
        } else if (current == CombatState.PREPARING_ATTACK) {
            result = attemptChamber(incoming);
        }

        if (result != DefenseResult.NONE) {
            lastHandledAttackId = incoming.id();
        }
        return result;
    }

    /**
     * Evaluates {@code incoming} for a Perfect Block or Parry against the
     * defender's currently locked guard direction. A Parry is a Perfect
     * Block whose timing also fell within the tighter {@link
     * CombatConstants#PARRY_WINDOW_TICKS}, so both events fire for a
     * Parry, but only one state is ever entered.
     */
    private DefenseResult attemptBlockDefense(IncomingAttack incoming) {
        GuardDirection required = DirectionCompatibility.matchingGuard(incoming.direction());
        if (required == GuardDirection.NONE || blockController.getCurrentDirection() != required) {
            return DefenseResult.NONE;
        }

        int deviation = Math.abs(incoming.ticksUntilImpact());
        if (deviation > CombatConstants.PERFECT_BLOCK_WINDOW_TICKS) {
            return DefenseResult.NONE;
        }

        boolean withinParryWindow = deviation <= CombatConstants.PARRY_WINDOW_TICKS;
        CombatState target = withinParryWindow ? CombatState.PARRY : CombatState.PERFECT_BLOCK;

        if (!stateManager.transitionTo(target)) {
            return DefenseResult.NONE;
        }

        transitionTicksRemaining = withinParryWindow
                ? CombatConstants.PARRY_STATE_DURATION_TICKS
                : CombatConstants.PERFECT_BLOCK_STATE_DURATION_TICKS;

        CombatEvents.PERFECT_BLOCK.invoker()
                .onPerfectBlock(new PerfectBlockEvent(player, incoming.attacker(), required, incoming.direction()));

        if (withinParryWindow) {
            CombatEvents.PARRY.invoker()
                    .onParry(new ParryEvent(player, incoming.attacker(), required, incoming.direction()));
            return DefenseResult.PARRY;
        }
        return DefenseResult.PERFECT_BLOCK;
    }

    /**
     * Evaluates {@code incoming} for a Chamber against the defender's
     * committed attack direction. Direction is checked immediately;
     * timing is recorded via {@link ChamberController} and resolved a
     * few ticks later by {@link #resolveChamberOutcome()} once {@code
     * CHAMBER_PREPARE}'s hold duration elapses, so the prepare animation
     * always gets a chance to play regardless of outcome.
     */
    private DefenseResult attemptChamber(IncomingAttack incoming) {
        if (incoming.direction() == AttackDirection.NONE || attackDirection != incoming.direction()) {
            return DefenseResult.NONE;
        }

        if (!stateManager.transitionTo(CombatState.CHAMBER_PREPARE)) {
            return DefenseResult.NONE;
        }

        boolean timingSuccess = Math.abs(incoming.ticksUntilImpact()) <= CombatConstants.CHAMBER_WINDOW_TICKS;
        transitionTicksRemaining = CombatConstants.CHAMBER_PREPARE_DURATION_TICKS;
        chamberController.begin(incoming, timingSuccess);

        CombatEvents.CHAMBER_STARTED.invoker()
                .onChamberStarted(new ChamberStartedEvent(player, incoming.attacker(), incoming.direction()));

        return DefenseResult.CHAMBER_STARTED;
    }

    /**
     * Resolves a pending chamber attempt once {@code CHAMBER_PREPARE}'s
     * hold duration elapses: advances to {@code CHAMBER_SUCCESS} and
     * fires {@link ChamberSucceededEvent} if timing was within {@link
     * CombatConstants#CHAMBER_WINDOW_TICKS}, otherwise reverts to {@code
     * PREPARING_ATTACK} so the wind-up simply continues uninterrupted.
     */
    private void resolveChamberOutcome() {
        boolean success = chamberController.wasTimingSuccessful();
        PlayerEntity attacker = chamberController.getPendingAttacker();
        AttackDirection direction = chamberController.getPendingDirection();
        chamberController.reset();

        if (success && stateManager.transitionTo(CombatState.CHAMBER_SUCCESS)) {
            transitionTicksRemaining = CombatConstants.CHAMBER_SUCCESS_DURATION_TICKS;
            CombatEvents.CHAMBER_SUCCEEDED.invoker()
                    .onChamberSucceeded(new ChamberSucceededEvent(player, attacker, direction));
        } else {
            stateManager.transitionTo(CombatState.PREPARING_ATTACK);
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

    /**
     * @return the item currently held in the main hand, or {@code null}
     * for an empty hand.
     */
    public Item getEquippedWeaponItem() {
        return weaponController.getCurrentItem();
    }

    /**
     * @return the combat stats currently governing this player — either
     * a registered weapon's properties, or {@link WeaponProperties#unarmed()}.
     * Every future system that needs weapon-aware behavior (damage,
     * stamina, AI, animation variants) should read this rather than
     * inspecting the held {@link Item} directly.
     */
    public WeaponProperties getWeaponProperties() {
        return weaponController.getCurrentWeapon();
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
        updateEquippedWeapon();
        advanceTransitionTimers();
        movementModifierManager.tick(player);
        blockController.tick();
        animationController.tick(player, stateManager.getCurrentState(), movementMode, attackDirection, blockController.getCurrentDirection());
    }

    /**
     * Detects a main-hand item change via {@link WeaponController} and,
     * if one occurred, notifies the rest of the framework: fires {@link
     * WeaponUnequippedEvent}/{@link WeaponEquippedEvent}/{@link
     * WeaponChangedEvent} as appropriate, then re-validates the
     * currently committed attack direction and locked guard direction
     * against the newly equipped weapon's supported directions.
     * <p>
     * Runs every tick regardless of {@code CombatState} — a weapon swap
     * needs to be picked up even outside Combat Mode, e.g. so a player
     * who swaps weapons before entering combat immediately sees the new
     * weapon's stats once they do.
     */
    private void updateEquippedWeapon() {
        Item previousItem = weaponController.getCurrentItem();
        WeaponProperties previousWeapon = weaponController.getCurrentWeapon();

        if (!weaponController.tick(player)) {
            return;
        }

        Item newItem = weaponController.getCurrentItem();
        WeaponProperties newWeapon = weaponController.getCurrentWeapon();

        if (previousItem != null) {
            CombatEvents.WEAPON_UNEQUIPPED.invoker()
                    .onWeaponUnequipped(new WeaponUnequippedEvent(player, previousItem, previousWeapon));
        }
        if (newItem != null) {
            CombatEvents.WEAPON_EQUIPPED.invoker()
                    .onWeaponEquipped(new WeaponEquippedEvent(player, newItem, newWeapon));
        }
        CombatEvents.WEAPON_CHANGED.invoker()
                .onWeaponChanged(new WeaponChangedEvent(player, previousItem, previousWeapon, newItem, newWeapon));

        revalidateDirectionsForWeapon(newWeapon);
    }

    /**
     * Called immediately after a weapon change resolves. If the new
     * weapon no longer supports the attack direction currently committed
     * mid-wind-up, or the guard direction currently locked while
     * blocking, that direction is cleared back to {@code NONE} — the
     * same "available attacks"/"guard positions" update the design spec
     * calls for on equip/unequip — rather than leaving the player
     * committed to a direction their new weapon can't perform.
     */
    private void revalidateDirectionsForWeapon(WeaponProperties weapon) {
        if (stateManager.getCurrentState() == CombatState.PREPARING_ATTACK
                && attackDirection != AttackDirection.NONE
                && !weapon.supportsAttackDirection(attackDirection)) {
            AttackDirection previous = attackDirection;
            attackDirection = AttackDirection.NONE;
            CombatEvents.ATTACK_DIRECTION_CHANGED.invoker()
                    .onAttackDirectionChanged(new AttackDirectionChangedEvent(player, previous, AttackDirection.NONE));
        }

        CombatState current = stateManager.getCurrentState();
        GuardDirection currentGuard = blockController.getCurrentDirection();
        if ((current == CombatState.ENTER_BLOCK || current == CombatState.BLOCK_IDLE)
                && currentGuard != GuardDirection.NONE
                && !weapon.supportsGuardDirection(currentGuard)) {
            blockController.reset();
            CombatEvents.GUARD_DIRECTION_CHANGED.invoker()
                    .onGuardDirectionChanged(new GuardDirectionChangedEvent(player, currentGuard, GuardDirection.NONE));
        }
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
            if (releaseBuffered && windUpTicksElapsed >= effectiveWindUpTicks()) {
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
        } else if (current == CombatState.PERFECT_BLOCK) {
            if (transitionTicksRemaining <= 0) {
                stateManager.transitionTo(CombatState.BLOCK_IDLE);
            } else {
                transitionTicksRemaining--;
            }
        } else if (current == CombatState.PARRY) {
            if (transitionTicksRemaining <= 0) {
                stateManager.transitionTo(CombatState.COMBAT_IDLE);
            } else {
                transitionTicksRemaining--;
            }
        } else if (current == CombatState.CHAMBER_PREPARE) {
            if (transitionTicksRemaining <= 0) {
                resolveChamberOutcome();
            } else {
                transitionTicksRemaining--;
            }
        } else if (current == CombatState.CHAMBER_SUCCESS) {
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
            transitionTicksRemaining = effectiveReleaseTicks();
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
            transitionTicksRemaining = effectiveRecoveryTicks();
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

    // ------------------------------------------------------------------
    // Weapon-scaled timing - the attack pipeline itself stays generic
    // (CombatConstants holds the unarmed/base values); these helpers are
    // the only place base timing is combined with the equipped weapon's
    // modifiers, per CombatConstants.DEFAULT_RECOVERY_DURATION_MODIFIER's
    // and WeaponProperties' documented intent.
    // ------------------------------------------------------------------

    /**
     * @return the minimum wind-up duration, in ticks, before a buffered
     * release is honored, scaled by the equipped weapon's {@link
     * WeaponProperties#windUpModifier()}.
     */
    private int effectiveWindUpTicks() {
        return scaledTicks(CombatConstants.MIN_ATTACK_PREPARATION_TICKS, weaponController.getCurrentWeapon().windUpModifier());
    }

    /**
     * @return the {@code ATTACKING} state's duration, in ticks, scaled by
     * whichever of the equipped weapon's {@link
     * WeaponProperties#swingSpeedModifier()} or {@link
     * WeaponProperties#thrustSpeedModifier()} applies to the currently
     * committed {@link #attackDirection}.
     */
    private int effectiveReleaseTicks() {
        WeaponProperties weapon = weaponController.getCurrentWeapon();
        double modifier = attackDirection == AttackDirection.THRUST
                ? weapon.thrustSpeedModifier()
                : weapon.swingSpeedModifier();
        return scaledTicks(CombatConstants.ATTACK_RELEASE_DURATION_TICKS, modifier);
    }

    /**
     * @return the {@code RECOVERY} state's duration, in ticks, scaled by
     * the equipped weapon's {@link WeaponProperties#recoveryModifier()}.
     */
    private int effectiveRecoveryTicks() {
        return scaledTicks(CombatConstants.RECOVERY_DURATION_TICKS, weaponController.getCurrentWeapon().recoveryModifier());
    }

    /**
     * Applies a weapon modifier to a base tick count, rounding to the
     * nearest tick and clamping to a minimum of 1 so no weapon modifier
     * can ever collapse a timing window to zero or negative ticks.
     */
    private static int scaledTicks(int baseTicks, double modifier) {
        return Math.max(1, (int) Math.round(baseTicks * modifier));
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