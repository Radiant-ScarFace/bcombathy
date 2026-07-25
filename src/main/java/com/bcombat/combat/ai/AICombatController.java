package com.bcombat.combat.ai;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.ai.group.CombatSquad;
import com.bcombat.combat.ai.group.SquadManager;
import com.bcombat.combat.ai.group.WeaponTactics;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.defense.DirectionCompatibility;
import com.bcombat.combat.state.CombatState;
import com.bcombat.combat.util.CombatConstants;
import com.bcombat.combat.weapon.WeaponProperties;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * The AI decision layer that lets a {@link MobEntity} fight through the
 * exact same {@link CombatController} public API a real player's {@code
 * CombatInputHandler} drives — requesting combat mode, committing wind-ups,
 * choosing attack/guard directions, releasing swings, raising and
 * lowering guards. This class contains no combat rules of its own: every
 * legality check (can this state be entered, does the weapon support
 * this direction, is stamina sufficient) is already enforced by {@link
 * CombatController} itself, exactly as it is for a human player. If a
 * decision made here is illegal, the corresponding {@code request*}/
 * {@code update*} call is simply a no-op — the same way a player
 * mashing an input at the wrong time is a no-op — so this class can
 * never bypass the state machine, attack pipeline, blocking, stamina,
 * collision, or damage systems.
 * <p>
 * What this class does NOT do:
 * <ul>
 *     <li>Target acquisition — it reads whatever {@link MobEntity#getTarget()}
 *     vanilla's own goal selector already picked. There is no duplicate
 *     "who to fight" system here. The one exception is squad-aware target
 *     selection (see below): when this AI belongs to a {@link CombatSquad},
 *     it may redirect vanilla's own target field to the squad's shared
 *     focus target — itself derived purely from every member's own
 *     vanilla-selected target — rather than inventing a second targeting
 *     system.</li>
 *     <li>Damage, collision, or hit resolution — those remain exactly
 *     {@code CollisionController}/{@code CollisionDetector}/{@code
 *     DamageService}'s responsibility, unchanged and unaware this class
 *     exists.</li>
 *     <li>Animation, movement-speed penalties, or stamina accounting —
 *     all still owned by {@link CombatController} and its sub-controllers.</li>
 * </ul>
 * What it does do, once per server tick via {@link #tick()}:
 * <ol>
 *     <li>Tactical positioning/distance management — approach, hold, or
 *     retreat via vanilla's own {@code EntityNavigation}, based on the
 *     equipped weapon's reach and the configured {@link AIDifficultyPreset}
 *     — additionally biased by {@link CombatRole} and {@link WeaponTactics}
 *     and, for a squad member, resolved into a flank/surround slot via
 *     {@link CombatSquad#flankPosition} instead of a straight approach.</li>
 *     <li>Facing — turns to face the target via vanilla's own {@code
 *     LookControl}, since {@code CollisionDetector#findTarget} requires
 *     the attacker to actually be looking at the target's forward cone,
 *     exactly like a player physically aiming their camera.</li>
 *     <li>Combat decisions — attacking, blocking, and defensive
 *     direction-matching, all expressed purely as calls into {@link
 *     CombatController}'s existing public API.</li>
 * </ol>
 * One instance exists per AI-controlled combatant, owned by {@link
 * AICombatManager} — never instantiated per-tick, and never holding a
 * second reference to combat state outside what {@link CombatController}
 * itself already exposes.
 * <p>
 * <b>Group Combat Framework integration:</b> every squad-facing addition
 * in this class (role/squad-aware distance and initiation bias, target
 * adoption, flanking/surrounding, spacing, friendly-fire avoidance,
 * mounted-charge-lane avoidance, retreat/regroup, low-health survival) is
 * gated on {@link #isInSquad()}, which is {@code false} whenever this AI
 * was constructed with a {@code null} role/squad id (the original
 * two-argument constructor) or {@link CombatConstants#GROUP_AI_ENABLED}
 * is off. A solo AI-controlled combatant therefore runs through exactly
 * the same decisions, in exactly the same order, producing exactly the
 * same result, as it always has — every squad branch below is additive,
 * never a replacement of the original solo code path.
 */
public final class AICombatController {

    private static final double NAVIGATION_SPEED = 1.0;
    private static final double RETREAT_SPEED = 0.9;
    private static final double APPROACH_SLACK = 0.75;
    private static final double RETREAT_TRIGGER_RATIO = 0.55;
    private static final double ENGAGEMENT_ENTRY_SLACK = 1.15;

    private static final Set<CombatState> COMMITTED_STATES = Set.of(
            CombatState.ATTACKING,
            CombatState.PERFECT_BLOCK,
            CombatState.PARRY,
            CombatState.CHAMBER_PREPARE,
            CombatState.CHAMBER_SUCCESS
    );

    private final MobEntity entity;
    private final CombatController combatController;
    private final AIDifficultyPreset difficulty;
    private final Random random = new Random();

    // Group Combat Framework: null/null for a solo (non-squad) AI, in
    // which case every squad-facing branch below is skipped entirely -
    // see isInSquad().
    private final CombatRole role;
    private final String squadId;

    private AITacticalIntent tacticalIntent = AITacticalIntent.IDLE;

    // Wind-up (PREPARING_ATTACK) bookkeeping.
    private boolean attackDirectionChosen = false;
    private int windUpTicksWaited = 0;

    // Reactive-block (COMBAT_IDLE) bookkeeping.
    private int opponentThreatTicks = 0;
    private boolean blockDecisionMade = false;

    // RECOVERY chain-attack bookkeeping.
    private boolean chainDecisionMade = false;

    // Mounted charge decision bookkeeping - reused/reset per mount state,
    // not per tick, so a committed charge decision persists across an
    // entire engagement rather than being re-rolled every tick.
    private boolean chargeDecisionMade = false;
    private boolean chargeCommitted = false;

    // Squad target-adoption bookkeeping - see considerSquadTarget().
    private LivingEntity lastSquadFocus = null;
    private int squadReactionTicksRemaining = 0;

    public AICombatController(MobEntity entity, AIDifficultyPreset difficulty) {
        this(entity, difficulty, null, null);
    }

    /**
     * Group Combat Framework overload: identical to the two-argument
     * constructor except this AI additionally opts into the given
     * {@link CombatRole}/squad id. Passing a {@code null} or blank
     * {@code squadId} (with or without a {@code role}) is equivalent to
     * the two-argument constructor — this AI is never tracked by {@link
     * SquadManager} and every squad branch in this class stays inert.
     * Joining/leaving the actual {@link CombatSquad} is {@link
     * AICombatManager#enable(MobEntity, AIDifficultyPreset, CombatRole, String)}'s
     * responsibility, not this constructor's — this class only ever
     * stores the role/squad id it was given.
     */
    public AICombatController(MobEntity entity, AIDifficultyPreset difficulty, CombatRole role, String squadId) {
        this.entity = Objects.requireNonNull(entity, "entity must not be null");
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
        // Same registry, same authoritative/non-authoritative rules as
        // every player - see CombatControllerManager's class docs.
        this.combatController = CombatControllerManager.get(entity);
        this.role = role;
        this.squadId = (squadId == null || squadId.isBlank()) ? null : squadId;
    }

    public MobEntity getEntity() {
        return entity;
    }

    /** @return the same {@link CombatController} a player's input handler would drive for this combatant. */
    public CombatController getCombatController() {
        return combatController;
    }

    public AIDifficultyPreset getDifficulty() {
        return difficulty;
    }

    /** @return this AI's {@link CombatRole}, or {@code null} if it isn't part of a squad. */
    public CombatRole getRole() {
        return role;
    }

    /** @return this AI's squad id, or {@code null} if it isn't part of a squad. */
    public String getSquadId() {
        return squadId;
    }

    /** @return the {@link CombatSquad} this AI currently belongs to, or {@code null} if it isn't part of one (including group AI being globally disabled). */
    public CombatSquad getSquad() {
        return isInSquad() ? SquadManager.get(squadId) : null;
    }

    /** @return this AI's last tactical positioning decision, for observability/debugging only. */
    public AITacticalIntent getTacticalIntent() {
        return tacticalIntent;
    }

    /**
     * Advances this AI's decision-making by one tick. Must be called
     * once per server tick by {@link AICombatManager} - separately from,
     * and before or after, {@link CombatController#tick()} itself (both
     * are ticked every server tick by the server tick loop; ordering
     * between the two does not matter since this class only ever issues
     * {@code request*}/{@code update*} calls that are queued/resolved by
     * {@link CombatController}'s own tick, never anything synchronous).
     */
    public void tick() {
        if (!entity.isAlive()) {
            return;
        }

        LivingEntity target = acquireTarget();
        if (target == null) {
            handleNoTarget();
            return;
        }

        CombatController opponentController = CombatControllerManager.getIfPresent(target);

        updateTacticalPositioning(target);
        updateFacing(target);
        driveCombatDecisions(target, opponentController);
    }

    // ------------------------------------------------------------------
    // Group Combat Framework gate
    // ------------------------------------------------------------------

    /**
     * @return true if this AI currently participates in group tactics:
     * it was constructed with a role and squad id, and {@link
     * CombatConstants#GROUP_AI_ENABLED} is on. Every squad-facing branch
     * in this class is gated on this method so a solo AI (the common
     * case, and the entirety of the original AI Combat Framework)
     * behaves exactly as it always has.
     */
    private boolean isInSquad() {
        return CombatConstants.GROUP_AI_ENABLED && squadId != null && role != null;
    }

    // ------------------------------------------------------------------
    // Target acquisition - deliberately NOT reimplemented for the solo
    // case. Vanilla's own goal selector (already present on every
    // hostile MobEntity) decides *who* to fight; this class only ever
    // reads that decision, except for the squad-aware adoption below.
    // ------------------------------------------------------------------

    private LivingEntity acquireTarget() {
        if (isInSquad()) {
            LivingEntity adopted = considerSquadTarget();
            if (adopted != null) {
                return adopted;
            }
        }
        LivingEntity target = entity.getTarget();
        return (target != null && target.isAlive()) ? target : null;
    }

    /**
     * Squad-aware target selection: if this AI's squad currently has a
     * live shared focus target (see {@link CombatSquad#getFocusTarget()}
     * — itself derived purely from every member's own vanilla-selected
     * target, never a second targeting system), redirects this entity's
     * own vanilla target field to it, so the whole squad converges on
     * the same threat rather than each member fighting whatever it
     * individually noticed first. Subject to a jittered reaction delay
     * (configurable via {@link CombatConstants#AI_REACTION_JITTER_TICKS})
     * so a squad doesn't visibly retarget in perfect lockstep the instant
     * its shared focus changes.
     *
     * @return the adopted focus target, or {@code null} if this AI
     * should fall back to its own {@code MobEntity#getTarget()} this tick.
     */
    private LivingEntity considerSquadTarget() {
        CombatSquad squad = getSquad();
        if (squad == null) {
            return null;
        }
        LivingEntity focus = squad.getFocusTarget();
        if (focus == null || !focus.isAlive() || squad.isAlly(this, focus)) {
            lastSquadFocus = null;
            squadReactionTicksRemaining = 0;
            return null;
        }

        if (focus != lastSquadFocus) {
            lastSquadFocus = focus;
            // Configurable reaction randomness: a fresh squad-wide
            // refocus is reacted to after a random 0..N tick delay
            // rather than instantly, so members don't all snap onto the
            // new target on the exact same tick.
            squadReactionTicksRemaining = random.nextInt(CombatConstants.AI_REACTION_JITTER_TICKS + 1);
        }

        if (squadReactionTicksRemaining > 0) {
            squadReactionTicksRemaining--;
            return null;
        }

        if (entity.getTarget() != focus) {
            entity.setTarget(focus);
        }
        return focus;
    }

    private void handleNoTarget() {
        if (combatController.getCombatState() != CombatState.NORMAL) {
            combatController.requestExitCombat();
        }
        if (!entity.getNavigation().isIdle()) {
            entity.getNavigation().stop();
        }
        resetEngagementBookkeeping();
        tacticalIntent = AITacticalIntent.IDLE;
    }

    private void resetEngagementBookkeeping() {
        attackDirectionChosen = false;
        windUpTicksWaited = 0;
        opponentThreatTicks = 0;
        blockDecisionMade = false;
        chainDecisionMade = false;
        chargeDecisionMade = false;
        chargeCommitted = false;
        lastSquadFocus = null;
        squadReactionTicksRemaining = 0;
    }

    // ------------------------------------------------------------------
    // Tactical positioning & distance management
    // ------------------------------------------------------------------

    private void updateTacticalPositioning(LivingEntity target) {
        CombatState state = combatController.getCombatState();

        if (COMMITTED_STATES.contains(state)) {
            // Mid-swing or mid-reaction-animation: never reposition out
            // from under the current action, exactly like a player
            // can't strafe-cancel a swing already committed.
            if (!entity.getNavigation().isIdle()) {
                entity.getNavigation().stop();
            }
            tacticalIntent = AITacticalIntent.COMMITTED;
            return;
        }

        updateMountedChargeDecision();

        double distance = entity.distanceTo(target);
        double idealDistance = computeIdealDistance();
        double approachThreshold = idealDistance + APPROACH_SLACK;

        CombatSquad squad = getSquad();
        boolean inSquad = squad != null;

        // Role-aware retreat trigger: retreatReluctance() > 1.0 divides
        // down into a lower effective threshold (retreats less readily),
        // < 1.0 divides up into a higher one (retreats more readily).
        // A solo AI (role == null) always uses the unmodified originals.
        double retreatTriggerRatio = RETREAT_TRIGGER_RATIO;
        double staminaCautionThreshold = difficulty.staminaCautionThreshold();
        if (inSquad) {
            retreatTriggerRatio = retreatTriggerRatio / role.retreatReluctance();
            staminaCautionThreshold = staminaCautionThreshold / role.retreatReluctance();
        }
        double retreatThreshold = idealDistance * retreatTriggerRatio;

        boolean shouldRetreat = distance < retreatThreshold
                && (combatController.isExhausted()
                || combatController.getStaminaRatio() < staminaCautionThreshold);

        // Low-health survival behavior: independent of, and stacked
        // alongside, the stamina-based trigger above - a squad member
        // critically wounded relative to its role's reluctance retreats
        // regardless of how much stamina it still has.
        if (inSquad) {
            double lowHealthThreshold = CombatConstants.SQUAD_LOW_HEALTH_RETREAT_RATIO / role.retreatReluctance();
            if (healthRatio(entity) < lowHealthThreshold) {
                shouldRetreat = true;
            }
        }

        if (shouldRetreat) {
            retreatFrom(target, squad);
            tacticalIntent = AITacticalIntent.RETREATING;
        } else if (inSquad) {
            positionWithSquad(target, squad, distance, idealDistance, approachThreshold);
        } else if (distance > approachThreshold) {
            entity.getNavigation().startMovingTo(target, NAVIGATION_SPEED);
            tacticalIntent = AITacticalIntent.APPROACHING;
        } else {
            if (!entity.getNavigation().isIdle()) {
                entity.getNavigation().stop();
            }
            tacticalIntent = AITacticalIntent.HOLDING;
        }

        // Entering combat mode is itself a tactical decision gated by
        // engagement range - done here (rather than unconditionally)
        // so a target that's merely visible, but far away, doesn't
        // immediately snap the AI into a combat stance.
        if (state == CombatState.NORMAL && distance <= difficulty.engagementRange()) {
            combatController.requestEnterCombat();
        }
    }

    /**
     * Flanking/surrounding/spacing/weapon-aware positioning for a squad
     * member: instead of approaching straight at {@code target} (the
     * solo behavior), navigates toward this AI's assigned flank/surround
     * slot around it, already nudged for ally spacing (via {@link
     * CombatSquad#flankPosition}) and away from any nearby ally's
     * mounted charge lane (via {@link CombatSquad#avoidMountedChargeLanes}).
     */
    private void positionWithSquad(LivingEntity target, CombatSquad squad, double distance,
                                   double idealDistance, double approachThreshold) {
        double flankRadius = idealDistance * CombatConstants.SQUAD_FLANK_RADIUS_RATIO;
        Vec3d desired = squad.flankPosition(this, target, flankRadius);
        desired = squad.avoidMountedChargeLanes(this, desired);

        double distanceToDesired = entity.getPos().distanceTo(desired);
        if (distanceToDesired > APPROACH_SLACK) {
            entity.getNavigation().startMovingTo(desired.x, desired.y, desired.z, NAVIGATION_SPEED);
            tacticalIntent = (distance > approachThreshold) ? AITacticalIntent.APPROACHING : AITacticalIntent.HOLDING;
        } else {
            if (!entity.getNavigation().isIdle()) {
                entity.getNavigation().stop();
            }
            tacticalIntent = AITacticalIntent.HOLDING;
        }
    }

    /**
     * @return this AI's ideal fighting distance for the current tick:
     * the original solo formula (weapon reach × {@link
     * AIDifficultyPreset#preferredDistanceRatio()}), additionally scaled
     * by {@link CombatRole#preferredDistanceMultiplier()} and {@link
     * WeaponTactics#preferredDistanceMultiplier(WeaponProperties)} for a
     * squad member, and floored at {@link CombatConstants#COUCH_AI_ENGAGE_DISTANCE}
     * while a mounted charge is committed. A solo AI (not in a squad,
     * not mounted-and-charging) gets back exactly the original formula.
     */
    private double computeIdealDistance() {
        double reach = combatController.getEffectiveReach();
        double idealDistance = Math.max(1.0, reach * difficulty.preferredDistanceRatio());

        if (isInSquad()) {
            idealDistance *= role.preferredDistanceMultiplier();
            idealDistance *= WeaponTactics.preferredDistanceMultiplier(combatController.getWeaponProperties());
        }

        if (chargeCommitted && combatController.isMounted()) {
            idealDistance = Math.max(idealDistance, CombatConstants.COUCH_AI_ENGAGE_DISTANCE);
        }

        return idealDistance;
    }

    /**
     * @return {@link AIDifficultyPreset#attackInitiationChance()},
     * additionally scaled by {@link CombatRole#attackInitiationMultiplier()}
     * and {@link WeaponTactics#attackInitiationMultiplier(WeaponProperties)}
     * for a squad member. A solo AI gets back exactly the original value.
     */
    private double computeAttackInitiationChance() {
        double chance = difficulty.attackInitiationChance();
        if (isInSquad()) {
            chance *= role.attackInitiationMultiplier();
            chance *= WeaponTactics.attackInitiationMultiplier(combatController.getWeaponProperties());
        }
        return chance;
    }

    /**
     * Mounted charge decision: once per engagement while mounted (reset
     * whenever this AI dismounts or loses its target), rolls whether to
     * commit to lining up a couched charge at all - eligibility (weapon,
     * global toggle, terrain, actual bracing/impact) remains entirely
     * {@code CouchLanceController}'s own concern via {@link
     * CombatController#requestPrepareAttack()}'s existing couch
     * interception; this method only ever decides whether the AI
     * *prefers* the longer {@link CombatConstants#COUCH_AI_ENGAGE_DISTANCE}
     * approach distance that gives a charge room to build speed, exactly
     * mirroring a rider's own decision to line up a charge rather than
     * duplicating anything the couch state machine already does.
     */
    private void updateMountedChargeDecision() {
        if (!combatController.isMounted()) {
            chargeDecisionMade = false;
            chargeCommitted = false;
            return;
        }
        if (chargeDecisionMade) {
            return;
        }
        chargeDecisionMade = true;

        WeaponProperties weapon = combatController.getWeaponProperties();
        boolean eligible = CombatConstants.COUCH_LANCE_ENABLED
                && weapon.isCouchCapable()
                && weapon.supportsAttackDirection(AttackDirection.THRUST);
        chargeCommitted = eligible && random.nextDouble() < CombatConstants.COUCH_AI_COUCH_CHANCE;
    }

    /**
     * Retreat & regroup behavior: identical to the original solo
     * "back straight away from the target" behavior when {@code squad}
     * is {@code null} (not in a squad). For a squad member with a live
     * {@link CombatSquad#getRegroupPoint()}, falls back toward it
     * instead - weighted by {@link CombatRole#regroupPriority()}, so a
     * {@code SUPPORT}-role member reliably heads for the group while an
     * {@code AGGRESSOR} more often just backs off locally.
     */
    private void retreatFrom(LivingEntity target, CombatSquad squad) {
        Vec3d destination = null;

        if (squad != null) {
            Vec3d regroupPoint = squad.getRegroupPoint();
            if (regroupPoint != null && random.nextDouble() < role.regroupPriority()) {
                destination = regroupPoint;
            }
        }

        if (destination == null) {
            Vec3d away = entity.getPos().subtract(target.getPos());
            if (away.lengthSquared() < 1.0E-4) {
                // Directly overlapping - pick an arbitrary retreat direction
                // rather than dividing by ~zero length.
                away = new Vec3d(1.0, 0.0, 0.0);
            }
            away = away.normalize();
            destination = entity.getPos().add(away.multiply(3.0));
        }

        entity.getNavigation().startMovingTo(destination.x, destination.y, destination.z, RETREAT_SPEED);
    }

    private void updateFacing(LivingEntity target) {
        // CollisionDetector#findTarget requires the attacker's actual
        // rotation to point at the target's forward cone - exactly like
        // a player physically aiming their camera - so this is not
        // optional cosmetic behavior: without it, this AI's own swings
        // would never register a hit.
        entity.getLookControl().lookAt(target, 30.0F, 30.0F);
    }

    // ------------------------------------------------------------------
    // Combat decision-making - every branch below only ever calls
    // CombatController's existing public API; every legality/timing/
    // stamina check already lives there and is never duplicated here.
    // ------------------------------------------------------------------

    private void driveCombatDecisions(LivingEntity target, CombatController opponentController) {
        CombatState state = combatController.getCombatState();

        switch (state) {
            case COMBAT_IDLE -> handleCombatIdle(target, opponentController);
            case PREPARING_ATTACK -> handlePreparingAttack(opponentController);
            case RECOVERY -> handleRecovery();
            case ENTER_BLOCK, BLOCK_IDLE -> handleBlocking(opponentController);
            default -> {
                // NORMAL/ENTERING_COMBAT/EXITING_COMBAT/ATTACKING/EXIT_BLOCK/
                // PERFECT_BLOCK/PARRY/CHAMBER_PREPARE/CHAMBER_SUCCESS/FEINT:
                // either already handled above (NORMAL's combat-entry is
                // decided in updateTacticalPositioning) or purely
                // time-driven - CombatController's own tick() advances
                // these without any AI input required, exactly as it
                // does for a player mid-animation.
            }
        }

        if (state != CombatState.COMBAT_IDLE) {
            opponentThreatTicks = 0;
            blockDecisionMade = false;
        }
        if (state != CombatState.PREPARING_ATTACK) {
            attackDirectionChosen = false;
            windUpTicksWaited = 0;
        }
        if (state != CombatState.RECOVERY) {
            chainDecisionMade = false;
        }
    }

    private boolean isOpponentThreatening(CombatController opponentController) {
        if (opponentController == null) {
            return false;
        }
        CombatState oppState = opponentController.getCombatState();
        return oppState == CombatState.PREPARING_ATTACK || oppState == CombatState.ATTACKING;
    }

    private void handleCombatIdle(LivingEntity target, CombatController opponentController) {
        boolean threatening = isOpponentThreatening(opponentController);

        if (threatening) {
            opponentThreatTicks++;
            if (!blockDecisionMade && opponentThreatTicks >= difficulty.reactionDelayTicks()) {
                blockDecisionMade = true;
                if (!combatController.isExhausted() && random.nextDouble() < difficulty.blockReactionChance()) {
                    combatController.requestEnterBlock();
                }
            }
            return;
        }

        opponentThreatTicks = 0;
        blockDecisionMade = false;

        if (combatController.isExhausted()
                || combatController.getStaminaRatio() < difficulty.staminaCautionThreshold()) {
            return;
        }

        double distance = entity.distanceTo(target);
        double idealDistance = computeIdealDistance();

        if (distance <= idealDistance * ENGAGEMENT_ENTRY_SLACK
                && random.nextDouble() < computeAttackInitiationChance()
                && !isFriendlyFireRisk(target)) {
            combatController.requestPrepareAttack();
        }
    }

    /**
     * Friendly-fire prevention: withholds this tick's attack initiation
     * if a squad-mate currently stands between this AI and {@code
     * target}. Always {@code false} for a solo (non-squad) AI, so it
     * never changes solo behavior.
     */
    private boolean isFriendlyFireRisk(LivingEntity target) {
        CombatSquad squad = getSquad();
        return squad != null && squad.isFriendlyFireRisk(this, target);
    }

    private void handlePreparingAttack(CombatController opponentController) {
        WeaponProperties weapon = combatController.getWeaponProperties();

        if (!attackDirectionChosen) {
            AttackDirection direction = chooseAttackDirection(weapon, opponentController);
            combatController.updateAttackDirection(direction);
            attackDirectionChosen = true;
            windUpTicksWaited = 0;
            return;
        }

        windUpTicksWaited++;
        if (windUpTicksWaited >= difficulty.reactionDelayTicks()) {
            // Safe to call every tick thereafter - CombatController
            // simply buffers this until its own minimum wind-up
            // duration elapses, exactly like a player clicking early.
            combatController.releaseAttack();
        }
    }

    private void handleRecovery() {
        if (chainDecisionMade) {
            return;
        }
        chainDecisionMade = true;
        if (!combatController.isExhausted() && random.nextDouble() < difficulty.chainAttackChance()) {
            combatController.bufferNextAttack();
        }
    }

    private void handleBlocking(CombatController opponentController) {
        boolean threatening = isOpponentThreatening(opponentController);

        GuardDirection desired = chooseGuardDirection(combatController.getWeaponProperties(), opponentController);
        if (desired != combatController.getGuardDirection()) {
            combatController.updateGuardDirection(desired);
        }

        if (combatController.getCombatState() == CombatState.BLOCK_IDLE
                && !threatening
                && random.nextDouble() < difficulty.exitBlockEagerness()) {
            combatController.requestExitBlock();
        }
    }

    // ------------------------------------------------------------------
    // Directional decision-making - reads the opponent's own
    // CombatController (if tracked) purely as information, exactly the
    // way a player visually reads an opponent's stance; never mutates it.
    // ------------------------------------------------------------------

    private AttackDirection chooseAttackDirection(WeaponProperties weapon, CombatController opponentController) {
        List<AttackDirection> supported = new ArrayList<>(weapon.supportedAttackDirections());
        if (supported.isEmpty()) {
            return AttackDirection.NONE;
        }

        if (opponentController != null) {
            CombatState oppState = opponentController.getCombatState();
            AttackDirection oppDirection = opponentController.getAttackDirection();

            // Chamber-trade: deliberately match the opponent's own
            // committed swing direction, per Bannerlord-style chamber
            // mechanics (CombatController#attemptChamber).
            if ((oppState == CombatState.PREPARING_ATTACK || oppState == CombatState.ATTACKING)
                    && oppDirection != AttackDirection.NONE
                    && supported.contains(oppDirection)
                    && random.nextDouble() < difficulty.chamberAttemptChance()) {
                return oppDirection;
            }

            // Weak-spot targeting: avoid whichever guard the opponent is
            // already holding.
            GuardDirection oppGuard = opponentController.getGuardDirection();
            if ((oppState == CombatState.ENTER_BLOCK || oppState == CombatState.BLOCK_IDLE)
                    && oppGuard != GuardDirection.NONE
                    && random.nextDouble() < difficulty.directionAccuracy()) {
                List<AttackDirection> unblocked = supported.stream()
                        .filter(direction -> DirectionCompatibility.matchingGuard(direction) != oppGuard)
                        .toList();
                if (!unblocked.isEmpty()) {
                    return unblocked.get(random.nextInt(unblocked.size()));
                }
            }
        }

        return supported.get(random.nextInt(supported.size()));
    }

    private GuardDirection chooseGuardDirection(WeaponProperties weapon, CombatController opponentController) {
        List<GuardDirection> supported = new ArrayList<>(weapon.supportedGuardDirections());
        if (supported.isEmpty()) {
            return GuardDirection.NONE;
        }

        if (opponentController != null) {
            CombatState oppState = opponentController.getCombatState();
            AttackDirection oppDirection = opponentController.getAttackDirection();

            if ((oppState == CombatState.PREPARING_ATTACK || oppState == CombatState.ATTACKING)
                    && oppDirection != AttackDirection.NONE
                    && random.nextDouble() < difficulty.directionAccuracy()) {
                GuardDirection matching = DirectionCompatibility.matchingGuard(oppDirection);
                if (supported.contains(matching)) {
                    return matching;
                }
            }
        }

        return supported.get(random.nextInt(supported.size()));
    }

    private static double healthRatio(LivingEntity entity) {
        float max = entity.getMaxHealth();
        if (max <= 0.0F) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, entity.getHealth() / max));
    }
}