package com.bcombat.combat.controller;

import com.bcombat.combat.animation.AnimationController;
import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.attack.ChamberController;
import com.bcombat.combat.block.BlockController;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.collision.CollisionController;
import com.bcombat.combat.collision.CollisionDetector;
import com.bcombat.combat.collision.CollisionOutcome;
import com.bcombat.combat.collision.HitLocation;
import com.bcombat.combat.collision.HitResult;
import com.bcombat.combat.defense.DefenseResult;
import com.bcombat.combat.defense.DirectionCompatibility;
import com.bcombat.combat.defense.IncomingAttack;
import com.bcombat.combat.events.AttackBlockedEvent;
import com.bcombat.combat.events.AttackDirectionChangedEvent;
import com.bcombat.combat.events.AttackHitEvent;
import com.bcombat.combat.events.AttackMissEvent;
import com.bcombat.combat.events.AttackPreparationCancelledEvent;
import com.bcombat.combat.events.AttackPreparationStartedEvent;
import com.bcombat.combat.events.AttackRecoveryStartedEvent;
import com.bcombat.combat.events.AttackReleasedEvent;
import com.bcombat.combat.events.BlockEndedEvent;
import com.bcombat.combat.events.BlockStartedEvent;
import com.bcombat.combat.events.ChamberStartedEvent;
import com.bcombat.combat.events.ChamberSucceededEvent;
import com.bcombat.combat.events.CollisionDetectedEvent;
import com.bcombat.combat.events.CombatEnterEvent;
import com.bcombat.combat.events.CombatEvents;
import com.bcombat.combat.events.CombatExitEvent;
import com.bcombat.combat.events.CombatStateChangedEvent;
import com.bcombat.combat.events.ExhaustionEndedEvent;
import com.bcombat.combat.events.ExhaustionStartedEvent;
import com.bcombat.combat.events.GuardDirectionChangedEvent;
import com.bcombat.combat.events.MovementModeChangedEvent;
import com.bcombat.combat.events.ParryEvent;
import com.bcombat.combat.events.PerfectBlockEvent;
import com.bcombat.combat.events.StaminaChangedEvent;
import com.bcombat.combat.events.StaminaDepletedEvent;
import com.bcombat.combat.events.StaminaRegeneratedEvent;
import com.bcombat.combat.events.WeaponChangedEvent;
import com.bcombat.combat.events.WeaponEquippedEvent;
import com.bcombat.combat.events.WeaponUnequippedEvent;
import com.bcombat.combat.movement.MovementMode;
import com.bcombat.combat.movement.MovementModifierManager;
import com.bcombat.combat.player.CombatModeGuard;
import com.bcombat.combat.player.CombatStance;
import com.bcombat.combat.stamina.ExhaustionState;
import com.bcombat.combat.stamina.StaminaController;
import com.bcombat.combat.state.CombatState;
import com.bcombat.combat.state.CombatStateManager;
import com.bcombat.combat.util.CombatConstants;
import com.bcombat.combat.weapon.WeaponController;
import com.bcombat.combat.weapon.WeaponProperties;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;

import java.util.UUID;
import java.util.Objects;

/**
 * The single per-combatant entry point for the combat framework. Every
 * future system (weapons, damage, stamina, AI, networking) should
 * interact with a combatant's combat state exclusively through this
 * class rather than reaching into {@link CombatStateManager}, {@link
 * MovementModifierManager}, or {@link AnimationController} directly.
 * <p>
 * Widened to {@link LivingEntity} (rather than {@link PlayerEntity})
 * so a single implementation drives both real players and AI-controlled
 * mobs through the exact same state machine, timing, collision, and
 * damage pipeline — see {@code com.bcombat.combat.ai.AICombatController}
 * for the AI decision layer that sits on top of this class and calls the
 * same public API {@code CombatInputHandler} does for a human player.
 * <p>
 * One instance exists per combatant, owned by {@link CombatControllerManager}.
 */
public final class CombatController {

    private final LivingEntity player;

    /**
     * True only for the single server-side instance that owns this
     * player's real combat outcome (a {@code ServerPlayerEntity} on the
     * logical server, including the integrated singleplayer server).
     * False for every client-side instance: the local player's
     * predictive copy (driven by input, corrected by {@link
     * #applySnapshot}/{@link #applyStaminaSnapshot}) and every remote
     * player's purely network-driven mirror.
     * <p>
     * This is the single flag that satisfies "server authority for
     * combat validation": collision detection and its resulting
     * hit/miss/blocked events - the only path that ever mutates health
     * via {@code DamageApplier} - are gated to authoritative instances
     * only (see {@link #tickCollision()} and the {@code ATTACKING}
     * branch of {@link #onStateTransition}). Every other subsystem
     * (state machine timing, animation, movement penalties, stamina)
     * intentionally still runs identically on both sides, since running
     * the same deterministic logic client-side is exactly what makes
     * local prediction and remote-player mirroring possible without any
     * bespoke replay/interpolation system.
     */
    private final boolean authoritative;

    private final CombatStateManager stateManager = new CombatStateManager();
    private final MovementModifierManager movementModifierManager = new MovementModifierManager();
    private final AnimationController animationController = new AnimationController();
    private final BlockController blockController = new BlockController();
    private final ChamberController chamberController = new ChamberController();
    private final WeaponController weaponController = new WeaponController();
    private final CollisionController collisionController = new CollisionController();
    private final StaminaController staminaController = new StaminaController();

    private MovementMode movementMode = MovementMode.NORMAL;
    private int transitionTicksRemaining = 0;

    private AttackDirection attackDirection = AttackDirection.NONE;
    private int windUpTicksElapsed = 0;
    private boolean releaseBuffered = false;
    private boolean nextAttackBuffered = false;

    /**
     * The weapon-scaled duration, in ticks, of the current (or most
     * recent) {@code ATTACKING} state — the same value used both for
     * {@code transitionTicksRemaining}'s countdown and to size {@link
     * CollisionController}'s detection window, so the collision window
     * automatically tracks weapon speed with no separate constant.
     */
    private int attackingDurationTicks = 0;

    /**
     * Identity of the last {@link IncomingAttack} this controller resolved
     * into a Perfect Block, Parry, or Chamber. Prevents the same physical
     * swing — which a future hit-detection system may re-notify more than
     * once as it closes in — from ever double-triggering a defensive
     * mechanic. See {@link #notifyIncomingAttack}.
     */
    private UUID lastHandledAttackId;

    public CombatController(LivingEntity player, boolean authoritative) {
        this.player = player;
        this.authoritative = authoritative;
        this.stateManager.setOnTransition(this::onStateTransition);
    }

    /** @return true if this is the server's authoritative instance for {@code player} - see {@link #authoritative}. */
    public boolean isAuthoritative() {
        return authoritative;
    }

    /** @return the combatant (player or AI-controlled mob) this controller drives. */
    public LivingEntity getEntity() {
        return player;
    }

    /**
     * @return the combatant as a {@link PlayerEntity}, or {@code null} if
     * this controller drives an AI-controlled mob instead. Convenience
     * for call sites that only make sense for a real player (HUD,
     * client-side prediction, networking) — prefer {@link #getEntity()}
     * for anything that should also work for AI.
     */
    public PlayerEntity getPlayer() {
        return player instanceof PlayerEntity p ? p : null;
    }

    /** @return true if this controller drives a real player rather than an AI-controlled mob. */
    public boolean isPlayer() {
        return player instanceof PlayerEntity;
    }

    // ------------------------------------------------------------------
    // Networking - snapshot capture/apply. Capture is only ever called
    // on the authoritative (server) instance, by com.bcombat.network's
    // per-tick broadcaster. Apply is only ever called on a
    // non-authoritative instance (the local player's predictive copy, or
    // a remote player's purely network-driven mirror), by the client's
    // packet receiver - see CombatSyncSnapshot/StaminaSyncSnapshot's
    // class docs for the full rationale.
    // ------------------------------------------------------------------

    /**
     * Captures a wire-friendly snapshot of everything a receiving client
     * needs to bring itself in line with this authoritative controller.
     * See {@link CombatSyncSnapshot}'s class docs for exactly what is
     * (and deliberately isn't) included.
     */
    public CombatSyncSnapshot captureSnapshot() {
        return new CombatSyncSnapshot(
                player.getUuid(),
                stateManager.getCurrentState(),
                attackDirection,
                blockController.getCurrentDirection(),
                movementMode,
                transitionTicksRemaining
        );
    }

    /**
     * Applies a {@link CombatSyncSnapshot} received from the server,
     * bringing this non-authoritative controller's state, committed
     * attack direction, locked guard direction, movement mode, and
     * transition timer in line with the server's authoritative copy.
     * No-op on the authoritative instance itself, which is always the
     * source of truth and must never be second-guessed by a stale/looped
     * packet.
     * <p>
     * The state transition is applied via {@link
     * CombatStateManager#forceTransitionTo}, bypassing the transition
     * graph entirely, since the server has already validated the
     * transition and this instance must simply mirror it rather than
     * risk silently rejecting a legitimate authoritative change (exactly
     * the same reasoning documented on {@link
     * BlockController#forceDirection}). This still fires {@link
     * #onStateTransition} and therefore the usual {@code
     * CombatStateChangedEvent} and friends, so animation and HUD code
     * react identically regardless of whether the transition happened
     * locally or arrived over the network - only the movement-speed
     * side effects inside {@link #onStateTransition} are skipped, since
     * those are gated to {@link #authoritative} directly.
     */
    public void applySnapshot(CombatSyncSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (authoritative) {
            return;
        }
        if (!snapshot.playerId().equals(player.getUuid())) {
            return;
        }

        if (snapshot.state() != stateManager.getCurrentState()) {
            stateManager.forceTransitionTo(snapshot.state());
        }

        if (attackDirection != snapshot.attackDirection()) {
            AttackDirection previous = attackDirection;
            attackDirection = snapshot.attackDirection();
            CombatEvents.ATTACK_DIRECTION_CHANGED.invoker()
                    .onAttackDirectionChanged(new AttackDirectionChangedEvent(player, previous, attackDirection));
        }

        if (blockController.getCurrentDirection() != snapshot.guardDirection()) {
            GuardDirection previous = blockController.getCurrentDirection();
            blockController.forceDirection(snapshot.guardDirection());
            CombatEvents.GUARD_DIRECTION_CHANGED.invoker()
                    .onGuardDirectionChanged(new GuardDirectionChangedEvent(player, previous, snapshot.guardDirection()));
        }

        setMovementMode(snapshot.movementMode());
        transitionTicksRemaining = snapshot.transitionTicksRemaining();
    }

    /**
     * Captures a wire-friendly snapshot of this authoritative
     * controller's current stamina/exhaustion, for the server's
     * lower-frequency, throttled stamina broadcast. See {@link
     * StaminaSyncSnapshot}'s class docs.
     */
    public StaminaSyncSnapshot captureStaminaSnapshot() {
        return new StaminaSyncSnapshot(
                player.getUuid(),
                staminaController.getCurrentStamina(),
                staminaController.getMaxStamina(),
                staminaController.isExhausted()
        );
    }

    /**
     * Applies a {@link StaminaSyncSnapshot} received from the server via
     * {@link StaminaController#applyAuthoritative}, and reports the
     * result through {@link #reportStaminaChange} exactly like a locally
     * driven stamina change would, so HUD/sound listeners never need to
     * distinguish a networked correction from a local one. No-op on the
     * authoritative instance itself.
     */
    public void applyStaminaSnapshot(StaminaSyncSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (authoritative) {
            return;
        }
        if (!snapshot.playerId().equals(player.getUuid())) {
            return;
        }

        double previousStamina = staminaController.getCurrentStamina();
        ExhaustionState previousExhaustion = staminaController.getExhaustionState();

        staminaController.applyAuthoritative(
                snapshot.currentStamina(),
                snapshot.maxStamina(),
                snapshot.exhausted() ? ExhaustionState.EXHAUSTED : ExhaustionState.NORMAL
        );

        reportStaminaChange(previousStamina, staminaController.getCurrentStamina(), previousExhaustion);
    }

    // ------------------------------------------------------------------
    // Stamina - a thin wrapper around StaminaController that turns every
    // consumption/regeneration into the StaminaChangedEvent/
    // StaminaDepletedEvent/StaminaRegeneratedEvent/ExhaustionStarted-
    // /EndedEvent quintet CombatEvents already declares for it. Kept
    // here (rather than inside StaminaController itself) for exactly
    // the same reason every other sub-controller stays event-agnostic -
    // see BlockController and ChamberController's class docs.
    // ------------------------------------------------------------------

    /**
     * Deducts {@code amount} stamina for a single discrete action (a
     * Perfect Block, a Parry, a Chamber, and - once a future attack
     * phase wires it in - an attack itself), via {@link
     * StaminaController#consume}, and reports the result through {@link
     * #reportStaminaChange}.
     */
    private void consumeStaminaForAction(double amount) {
        double previousStamina = staminaController.getCurrentStamina();
        ExhaustionState previousExhaustion = staminaController.getExhaustionState();

        double currentStamina = staminaController.consume(amount);

        reportStaminaChange(previousStamina, currentStamina, previousExhaustion);
    }

    /**
     * Fires {@link StaminaChangedEvent} if {@code currentStamina} differs
     * from {@code previousStamina}, plus whichever of {@link
     * StaminaDepletedEvent}, {@link StaminaRegeneratedEvent}, {@link
     * ExhaustionStartedEvent}, or {@link ExhaustionEndedEvent} the
     * transition also crossed - the single place both {@link
     * #consumeStaminaForAction} and {@link #tickStamina} report through,
     * so a future listener (HUD, sound, networking) subscribes once and
     * sees every stamina change the same way regardless of its cause.
     */
    private void reportStaminaChange(double previousStamina, double currentStamina, ExhaustionState previousExhaustion) {
        if (currentStamina == previousStamina) {
            return;
        }

        double maxStamina = staminaController.getMaxStamina();
        CombatEvents.STAMINA_CHANGED.invoker()
                .onStaminaChanged(new StaminaChangedEvent(player, previousStamina, currentStamina, maxStamina));

        if (previousStamina > 0.0 && currentStamina <= 0.0) {
            CombatEvents.STAMINA_DEPLETED.invoker().onStaminaDepleted(new StaminaDepletedEvent(player));
        } else if (previousStamina < maxStamina && currentStamina >= maxStamina) {
            CombatEvents.STAMINA_REGENERATED.invoker().onStaminaRegenerated(new StaminaRegeneratedEvent(player));
        }

        ExhaustionState currentExhaustion = staminaController.getExhaustionState();
        if (currentExhaustion != previousExhaustion) {
            if (currentExhaustion.isExhausted()) {
                // See the ENTERING_COMBAT branch of onStateTransition for
                // why every movement-speed mutation is confined to the
                // authoritative instance.
                if (authoritative) {
                    movementModifierManager.enableExhaustionPenalty(player);
                }
                CombatEvents.EXHAUSTION_STARTED.invoker().onExhaustionStarted(new ExhaustionStartedEvent(player));
            } else {
                if (authoritative) {
                    movementModifierManager.disableExhaustionPenalty(player);
                }
                CombatEvents.EXHAUSTION_ENDED.invoker().onExhaustionEnded(new ExhaustionEndedEvent(player));
            }
        }
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
     * wind-up. Only succeeds from {@code COMBAT_IDLE}, and only while
     * stamina is not {@link ExhaustionState#EXHAUSTED} — a new attack
     * can never be started while exhausted, though one already mid-swing
     * always completes.
     */
    public void requestPrepareAttack() {
        if (staminaController.isExhausted()) {
            return;
        }
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
     * the state machine and needs no extra bookkeeping here. Also
     * refused while stamina is {@link ExhaustionState#EXHAUSTED} — a new
     * block can never be raised while exhausted.
     */
    public void requestEnterBlock() {
        if (staminaController.isExhausted()) {
            return;
        }
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

        consumeStaminaForAction(withinParryWindow
                ? CombatConstants.PARRY_STAMINA_COST
                : CombatConstants.PERFECT_BLOCK_STAMINA_COST);

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
        LivingEntity attacker = chamberController.getPendingAttacker();
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

    /**
     * Turns one attack's resolved {@link CollisionOutcome} into the
     * appropriate event(s). A found target does not automatically mean a
     * confirmed hit: if the target already has a tracked {@link
     * CombatController} — a real player, or an AI-controlled combatant
     * driven by {@code com.bcombat.combat.ai.AICombatController} — this
     * reuses {@link #notifyIncomingAttack} exactly the way a real
     * hit-detection system is documented to — with the collision itself
     * as the instant of impact ({@code ticksUntilImpact == 0}) — so
     * Perfect Block/Parry/Chamber intercept this attack the same way
     * they already intercept {@code DefenseTestSimulator}'s simulated
     * ones, regardless of whether the defender is a player or AI. A
     * plain vanilla mob with no tracked controller is never given one
     * just to be hit — {@link CombatControllerManager#getIfPresent} is
     * used here rather than {@link CombatControllerManager#get} so this
     * lookup can never itself create one. A successful interception
     * fires {@link AttackBlockedEvent} and skips {@link AttackHitEvent}
     * entirely, per the requirement that a blocked attack never also
     * confirms as a hit.
     */
    private void resolveCollisionOutcome(CollisionOutcome outcome) {
        Item weaponItem = weaponController.getCurrentItem();
        WeaponProperties weapon = weaponController.getCurrentWeapon();
        long worldTime = player.getWorld().getTime();

        if (!outcome.hasTarget()) {
            HitResult result = HitResult.miss(player, weaponItem, weapon, attackDirection, outcome.ticksIntoAttack(), worldTime);
            CombatEvents.ATTACK_MISS.invoker().onAttackMiss(new AttackMissEvent(result));
            return;
        }

        LivingEntity target = outcome.target();
        CombatEvents.COLLISION_DETECTED.invoker()
                .onCollisionDetected(new CollisionDetectedEvent(player, target, attackDirection, weaponItem));

        CombatController defenderController = CombatControllerManager.getIfPresent(target);
        if (defenderController != null) {
            DefenseResult defenseResult = defenderController.notifyIncomingAttack(new IncomingAttack(player, attackDirection, 0));
            if (defenseResult != DefenseResult.NONE) {
                HitResult result = HitResult.blocked(player, target, weaponItem, weapon, attackDirection, defenseResult, outcome.ticksIntoAttack(), worldTime);
                CombatEvents.ATTACK_BLOCKED.invoker().onAttackBlocked(new AttackBlockedEvent(result));
                return;
            }
        }

        HitLocation hitLocation = CollisionDetector.classifyHitLocation(player, target, attackDirection);
        HitResult result = HitResult.hit(player, target, weaponItem, weapon, attackDirection, hitLocation, outcome.ticksIntoAttack(), worldTime);
        CombatEvents.ATTACK_HIT.invoker().onAttackHit(new AttackHitEvent(result));
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
    // Stamina read-only accessors — the public surface AICombatController
    // (and any future HUD) uses to make stamina-aware decisions without
    // ever touching StaminaController directly. Mirrors the read-only
    // accessors already exposed above for combat state, movement mode,
    // and weapon; no new mutation entry point is added here, so stamina
    // can still only ever change via consumeStaminaForAction/tickStamina/
    // applyStaminaSnapshot exactly as before.
    // ------------------------------------------------------------------

    /** @return current stamina, 0..{@link #getMaxStamina()}. */
    public double getCurrentStamina() {
        return staminaController.getCurrentStamina();
    }

    /** @return this combatant's maximum stamina. */
    public double getMaxStamina() {
        return staminaController.getMaxStamina();
    }

    /** @return current stamina as a fraction (0-1) of maximum. */
    public double getStaminaRatio() {
        return staminaController.getStaminaRatio();
    }

    /** @return true if this combatant is currently {@link ExhaustionState#EXHAUSTED}. */
    public boolean isExhausted() {
        return staminaController.isExhausted();
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
        // See the ENTERING_COMBAT branch of onStateTransition for why
        // every movement-speed mutation is confined to the authoritative
        // instance.
        if (authoritative) {
            movementModifierManager.tick(player);
        }
        blockController.tick();
        tickCollision();
        tickStamina();
        animationController.tick(player, stateManager.getCurrentState(), movementMode, attackDirection, blockController.getCurrentDirection());
    }

    /**
     * Advances stamina regeneration by one tick via {@link
     * StaminaController#tick}, honoring the equipped weapon's regen
     * modifiers exactly the way {@link #effectiveWindUpTicks()} and
     * friends already combine weapon modifiers with base timing.
     * Regeneration is suspended while the player is actively spending
     * effort (mid wind-up, mid swing, or holding a block) - see {@link
     * #isStaminaRegenSuspended()}. Reports the result through {@link
     * #reportStaminaChange} exactly like {@link #consumeStaminaForAction}
     * does, so listeners never need to distinguish consumption from
     * regeneration.
     */
    private void tickStamina() {
        double previousStamina = staminaController.getCurrentStamina();
        ExhaustionState previousExhaustion = staminaController.getExhaustionState();

        WeaponProperties weapon = weaponController.getCurrentWeapon();
        staminaController.tick(isStaminaRegenSuspended(), weapon.staminaRegenDelayModifier(), weapon.staminaRegenRateModifier());

        reportStaminaChange(previousStamina, staminaController.getCurrentStamina(), previousExhaustion);
    }

    /**
     * @return true while the current combat state represents actively
     * spending effort - mid wind-up, mid swing, or holding a raised
     * guard - during which stamina regeneration is withheld regardless
     * of the post-consumption delay {@link StaminaController} also
     * tracks internally.
     */
    private boolean isStaminaRegenSuspended() {
        CombatState state = stateManager.getCurrentState();
        return state == CombatState.PREPARING_ATTACK
                || state == CombatState.ATTACKING
                || state == CombatState.ENTER_BLOCK
                || state == CombatState.BLOCK_IDLE;
    }

    /**
     * Polls the active collision window, if any, while {@code
     * CombatState.ATTACKING} is current. See {@link CollisionController}
     * for why detection is windowed rather than checked every tick of
     * the release phase.
     */
    private void tickCollision() {
        // Server authority: only the authoritative instance ever detects
        // a hit, since resolveCollisionOutcome() is the sole path that
        // fires AttackHitEvent/AttackBlockedEvent/AttackMissEvent, and
        // DamageService reacts to AttackHitEvent by mutating real health.
        // A non-authoritative instance (local prediction or a remote
        // mirror) never runs its own collision detection at all - it
        // only ever learns the outcome once the server's hit/miss/block
        // events reach it through whatever future feedback packet a
        // client listens for (see CombatSyncSnapshot/StaminaSyncSnapshot's
        // docs and com.bcombat.network's class docs).
        if (!authoritative) {
            return;
        }
        if (stateManager.getCurrentState() != CombatState.ATTACKING) {
            return;
        }
        collisionController.tick(player, weaponController.getCurrentWeapon().reach())
                .ifPresent(this::resolveCollisionOutcome);
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
        // Computed before the transition (rather than after, like other
        // states' durations) since onStateTransition's ATTACKING branch
        // needs this value the instant the transition fires, to size
        // CollisionController's detection window.
        attackingDurationTicks = effectiveReleaseTicks();
        if (stateManager.transitionTo(CombatState.ATTACKING)) {
            transitionTicksRemaining = attackingDurationTicks;
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
            // Movement speed is a synchronized entity attribute: mutating
            // it only on the authoritative (server) instance and letting
            // vanilla's own attribute sync push the result to every
            // client - including the owning client's own player entity -
            // is what keeps this correct in multiplayer without a
            // bespoke packet. See MovementModifierManager's class docs.
            if (authoritative) {
                movementModifierManager.enableCombatMovement(player);
            }
            CombatEvents.COMBAT_ENTER.invoker().onCombatEnter(new CombatEnterEvent(player));
        }

        if (current == CombatState.EXITING_COMBAT) {
            transitionTicksRemaining = CombatConstants.EXIT_COMBAT_TRANSITION_TICKS;
            CombatEvents.COMBAT_EXIT.invoker().onCombatExit(new CombatExitEvent(player));
        }

        if (current == CombatState.NORMAL) {
            setMovementMode(MovementMode.NORMAL);
            if (authoritative) {
                movementModifierManager.disableCombatMovement(player);
            }
            nextAttackBuffered = false;
            blockController.reset();
        }

        if (current == CombatState.PREPARING_ATTACK) {
            if (authoritative) {
                movementModifierManager.enableWindUpPenalty(player);
            }
        } else if (previous == CombatState.PREPARING_ATTACK) {
            // Covers every way a wind-up can end: release into ATTACKING,
            // a cancelled wind-up back to COMBAT_IDLE, or a forced exit.
            // disableCombatMovement() above already strips this when the
            // path leads all the way to NORMAL, but that call only fires
            // for that specific branch, so this is the single place that
            // guarantees the penalty never outlives PREPARING_ATTACK.
            if (authoritative) {
                movementModifierManager.disableWindUpPenalty(player);
            }
        }

        // Server authority: collision windows only ever open/resolve on
        // the authoritative instance - see tickCollision()'s docs for
        // why. A non-authoritative instance still transitions through
        // ATTACKING on schedule (for animation/prediction), it just
        // never opens a detection window for it.
        if (authoritative) {
            if (current == CombatState.ATTACKING) {
                // Collision checks only ever run during the release phase
                // (ATTACKING) - see CollisionController's class docs.
                collisionController.beginWindow(attackingDurationTicks);
            } else if (previous == CombatState.ATTACKING) {
                // Guarantees every attack resolves to a hit/blocked/miss
                // outcome even if ATTACKING's duration is short enough that
                // tick() never gets a chance to close the window itself;
                // a no-op if tick() already resolved it naturally.
                collisionController.forceResolve().ifPresent(this::resolveCollisionOutcome);
                collisionController.reset();
            }
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